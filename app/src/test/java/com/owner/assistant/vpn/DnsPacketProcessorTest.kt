package com.owner.assistant.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.InetAddress

/**
 * Pure-JVM tests for the hand-rolled IPv4/UDP/DNS (de)serialization behind
 * the ad-block sinkhole — builds synthetic packets byte-for-byte and checks
 * what [DnsPacketProcessor] does with them, no VPN/device needed.
 */
class DnsPacketProcessorTest {

    private val clientIp = InetAddress.getByAddress(byteArrayOf(10, 111, 222, 5))
    private val dnsServerIp = InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))
    private val clientPort = 54321

    private fun dnsQuestionBytes(domain: String): ByteArray {
        val out = ByteArrayOutputStream()
        domain.split(".").forEach { label ->
            out.write(label.length)
            out.write(label.toByteArray(Charsets.US_ASCII))
        }
        out.write(0) // root terminator
        out.write(byteArrayOf(0x00, 0x01)) // QTYPE = A
        out.write(byteArrayOf(0x00, 0x01)) // QCLASS = IN
        return out.toByteArray()
    }

    private fun dnsQueryMessage(domain: String, transactionId: Int = 0x1234): ByteArray {
        val header = byteArrayOf(
            (transactionId shr 8).toByte(), transactionId.toByte(),
            0x01, 0x00, // flags: standard query, recursion desired
            0x00, 0x01, // QDCOUNT = 1
            0x00, 0x00, // ANCOUNT = 0
            0x00, 0x00, // NSCOUNT = 0
            0x00, 0x00  // ARCOUNT = 0
        )
        return header + dnsQuestionBytes(domain)
    }

    /** Hand-builds a full IPv4/UDP/DNS packet the same shape [AdBlockVpnService] reads off the TUN. */
    private fun buildIpv4UdpPacket(
        srcIp: InetAddress, srcPort: Int, dstIp: InetAddress, dstPort: Int,
        protocol: Int = 17, payload: ByteArray
    ): ByteArray {
        val udpLength = 8 + payload.size
        val totalLength = 20 + udpLength
        val packet = ByteArray(totalLength)

        packet[0] = 0x45
        packet[2] = (totalLength shr 8).toByte()
        packet[3] = totalLength.toByte()
        packet[8] = 64
        packet[9] = protocol.toByte()
        System.arraycopy(srcIp.address, 0, packet, 12, 4)
        System.arraycopy(dstIp.address, 0, packet, 16, 4)

        packet[20] = (srcPort shr 8).toByte(); packet[21] = srcPort.toByte()
        packet[22] = (dstPort shr 8).toByte(); packet[23] = dstPort.toByte()
        packet[24] = (udpLength shr 8).toByte(); packet[25] = udpLength.toByte()
        // UDP checksum left as 0 (valid "not computed" for IPv4)

        System.arraycopy(payload, 0, packet, 28, payload.size)
        return packet
    }

    @Test
    fun `parses domain name out of a well-formed DNS query packet`() {
        val packet = buildIpv4UdpPacket(
            clientIp, clientPort, dnsServerIp, 53, payload = dnsQueryMessage("example.com")
        )

        val query = DnsPacketProcessor.parse(packet, packet.size)

        assertTrue(query != null)
        assertEquals("example.com", query!!.domain)
        assertEquals(clientPort, query.sourcePort)
        assertEquals(53, query.destPort)
        assertEquals(clientIp, query.sourceIp)
        assertEquals(dnsServerIp, query.destIp)
    }

    @Test
    fun `rejects non-UDP packets`() {
        val packet = buildIpv4UdpPacket(
            clientIp, clientPort, dnsServerIp, 53, protocol = 6 /* TCP */, payload = dnsQueryMessage("example.com")
        )
        assertNull(DnsPacketProcessor.parse(packet, packet.size))
    }

    @Test
    fun `rejects packets not addressed to port 53`() {
        val packet = buildIpv4UdpPacket(
            clientIp, clientPort, dnsServerIp, 8080, payload = dnsQueryMessage("example.com")
        )
        assertNull(DnsPacketProcessor.parse(packet, packet.size))
    }

    @Test
    fun `rejects a packet shorter than the minimum header size`() {
        assertNull(DnsPacketProcessor.parse(ByteArray(10), 10))
    }

    @Test
    fun `blocked response sets the DNS response flag and answers with 0-point-0-point-0-point-0`() {
        val packet = buildIpv4UdpPacket(
            clientIp, clientPort, dnsServerIp, 53, payload = dnsQueryMessage("ads.example.com", 0x55AA)
        )
        val query = DnsPacketProcessor.parse(packet, packet.size)!!

        val response = DnsPacketProcessor.buildBlockedResponse(query)

        // Response should be addressed back to the original querier.
        assertEquals(dnsServerIp, InetAddress.getByAddress(response.copyOfRange(12, 16)))
        assertEquals(clientIp, InetAddress.getByAddress(response.copyOfRange(16, 20)))
        val responseSrcPort = ((response[20].toInt() and 0xFF) shl 8) or (response[21].toInt() and 0xFF)
        val responseDstPort = ((response[22].toInt() and 0xFF) shl 8) or (response[23].toInt() and 0xFF)
        assertEquals(53, responseSrcPort)
        assertEquals(clientPort, responseDstPort)

        val dns = response.copyOfRange(28, response.size)
        val transactionId = ((dns[0].toInt() and 0xFF) shl 8) or (dns[1].toInt() and 0xFF)
        assertEquals(0x55AA, transactionId)

        val flagsHighByte = dns[2].toInt() and 0xFF
        assertTrue("QR bit (response) should be set", flagsHighByte and 0x80 != 0)

        val answerCount = ((dns[6].toInt() and 0xFF) shl 8) or (dns[7].toInt() and 0xFF)
        assertEquals(1, answerCount)

        // Last 4 bytes of the packet are the answer's RDATA (the IPv4 address).
        val rdata = response.copyOfRange(response.size - 4, response.size)
        assertTrue("Blocked response should resolve to 0.0.0.0", rdata.all { it == 0.toByte() })
    }
}
