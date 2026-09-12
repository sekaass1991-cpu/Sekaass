package com.owner.assistant.vpn

import android.app.Notification
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.owner.assistant.AssistantApp
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Feature E: local DNS-based ad blocking, per the blueprint's `VpnService`
 * suggestion. Rather than tunneling *all* traffic (which would require
 * hand-writing a full user-space TCP/IP stack), this VPN routes only DNS
 * queries into the tunnel: it reads the device's configured DNS server(s)
 * and adds routes for just those IPs, so only port-53 traffic ever reaches
 * [DnsPacketProcessor]. Blocked domains get an instant synthetic 0.0.0.0
 * reply; everything else is relayed to the real DNS server and the reply is
 * repackaged back to the querying app. Regular web traffic never touches
 * this VPN and keeps going over the normal network path.
 *
 * Known limitation (documented in blueprint section 7 / README): apps that
 * hardcode a DNS-over-HTTPS resolver or their own DNS client ignoring the
 * system resolver can bypass this. In-app ads rendered by a remote ad SDK
 * that reuses a domain already resolved and cached also can't be blocked
 * after the fact without root.
 */
class AdBlockVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private val executor = Executors.newCachedThreadPool()
    private val writeLock = Any()

    override fun onCreate() {
        super.onCreate()
        Blocklist.load(this)
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (running.compareAndSet(false, true)) {
            establishTunnel()
            executor.execute { runPacketLoop() }
        }
        return START_STICKY
    }

    private fun establishTunnel() {
        val dnsServers = systemDnsServers().ifEmpty { listOf("1.1.1.1", "8.8.8.8") }

        val builder = Builder()
            .setSession("Personal Assistant Ad Block")
            .setMtu(1500)
            .addAddress(TUN_ADDRESS, 32)

        dnsServers.filter { isIpv4(it) }.forEach { dns ->
            builder.addRoute(dns, 32)
            builder.addDnsServer(dns)
        }

        tunInterface = builder.establish()
        Log.i(TAG, "Ad-block VPN established, filtering DNS via $dnsServers, ${Blocklist.size()} domains blocked")
    }

    private fun systemDnsServers(): List<String> {
        return try {
            val cm = getSystemService(ConnectivityManager::class.java)
            val network = cm.activeNetwork ?: return emptyList()
            cm.getLinkProperties(network)?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read system DNS servers", e)
            emptyList()
        }
    }

    private fun isIpv4(address: String): Boolean = address.count { it == '.' } == 3

    private fun runPacketLoop() {
        val pfd = tunInterface ?: return
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteArray(32_767)

        while (running.get()) {
            val length = try {
                input.read(buffer)
            } catch (e: Exception) {
                if (running.get()) Log.e(TAG, "Tunnel read failed", e)
                break
            }
            if (length <= 0) continue

            val packetCopy = buffer.copyOf(length)
            val query = DnsPacketProcessor.parse(packetCopy, length) ?: continue

            if (Blocklist.isBlocked(query.domain)) {
                writePacket(output, DnsPacketProcessor.buildBlockedResponse(query))
            } else {
                executor.execute { forwardAndReply(query, output) }
            }
        }
    }

    private fun forwardAndReply(query: DnsPacketProcessor.DnsQuery, output: FileOutputStream) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            protect(socket) // exempt this socket from re-entering the VPN
            socket.soTimeout = UPSTREAM_TIMEOUT_MS

            val request = DatagramPacket(
                query.dnsMessage, query.dnsMessage.size, InetSocketAddress(query.destIp, 53)
            )
            socket.send(request)

            val replyBuffer = ByteArray(512)
            val replyPacket = DatagramPacket(replyBuffer, replyBuffer.size)
            socket.receive(replyPacket)

            val reply = replyBuffer.copyOf(replyPacket.length)
            writePacket(output, DnsPacketProcessor.buildForwardedResponse(query, reply))
        } catch (e: Exception) {
            Log.w(TAG, "Upstream DNS lookup failed for ${query.domain}", e)
        } finally {
            socket?.close()
        }
    }

    private fun writePacket(output: FileOutputStream, packet: ByteArray) {
        synchronized(writeLock) {
            try {
                output.write(packet)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to write packet back to tunnel", e)
            }
        }
    }

    override fun onDestroy() {
        running.set(false)
        executor.shutdownNow()
        tunInterface?.close()
        tunInterface = null
        super.onDestroy()
    }

    override fun onRevoke() {
        stopSelf()
        super.onRevoke()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, AssistantApp.CHANNEL_SERVICE)
            .setContentTitle("Ad blocking active")
            .setContentText("Filtering DNS lookups against ${Blocklist.size()} known ad/tracker domains")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()

    companion object {
        private const val TAG = "AdBlockVpnService"
        private const val NOTIFICATION_ID = 4
        private const val TUN_ADDRESS = "10.111.222.1"
        private const val UPSTREAM_TIMEOUT_MS = 5000
    }
}
