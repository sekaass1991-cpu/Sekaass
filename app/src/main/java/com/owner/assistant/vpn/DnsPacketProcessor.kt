package com.owner.assistant.vpn

import java.net.InetAddress

/**
 * Minimal hand-rolled IPv4 + UDP + DNS (de)serialization for the ad-block
 * sinkhole. Only handles what [AdBlockVpnService] actually needs: reading a
 * DNS query out of a raw IP packet the TUN handed us, and building either a
 * synthetic "blocked" reply or a repackaged real reply to send back.
 *
 * IPv4 only — IPv6 DNS traffic is left untouched (see README limitations).
 */
object DnsPacketProcessor {

    data class DnsQuery(
        val sourceIp: InetAddress,
        val sourcePort: Int,
        val destIp: InetAddress,
        val destPort: Int,
        val dnsMessage: ByteArray,
        val domain: String
    )

    /** @return null if this isn't an IPv4/UDP/port-53 packet, or it's malformed. */
    fun parse(packet: ByteArray, length: Int): DnsQuery? {
        if (length < 28) return null // shorter than min IPv4(20) + UDP(8) header
        val versionAndIhl = packet[0].toInt() and 0xFF
        val version = versionAndIhl shr 4
        if (version != 4) return null
        val ihl = (versionAndIhl and 0x0F) * 4
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != PROTOCOL_UDP) return null

        val sourceIp = InetAddress.getByAddress(packet.copyOfRange(12, 16))
        val destIp = InetAddress.getByAddress(packet.copyOfRange(16, 20))

        val udpStart = ihl
        if (udpStart + 8 > length) return null
        val sourcePort = readUInt16(packet, udpStart)
        val destPort = readUInt16(packet, udpStart + 2)
        if (destPort != 53) return null

        val dnsStart = udpStart + 8
        if (dnsStart >= length) return null
        val dnsMessage = packet.copyOfRange(dnsStart, length)
        val domain = parseQuestionName(dnsMessage) ?: return null

        return DnsQuery(sourceIp, sourcePort, destIp, destPort, dnsMessage, domain)
    }

    /** Reads the first question's QNAME out of a DNS message (starts after the 12-byte header). */
    private fun parseQuestionName(dns: ByteArray): String? {
        if (dns.size < 13) return null
        val labels = mutableListOf<String>()
        var offset = 12
        while (offset < dns.size) {
            val len = dns[offset].toInt() and 0xFF
            if (len == 0) break
            if (offset + 1 + len > dns.size) return null
            labels.add(String(dns, offset + 1, len, Charsets.US_ASCII))
            offset += 1 + len
        }
        return if (labels.isEmpty()) null else labels.joinToString(".")
    }

    /** Builds a full IPv4/UDP/DNS packet answering [query] with 0.0.0.0, sent back to the querier. */
    fun buildBlockedResponse(query: DnsQuery): ByteArray {
        val header = query.dnsMessage.copyOfRange(0, 12)
        header[2] = (header[2].toInt() or 0x80).toByte() // QR = 1 (response)
        header[3] = (header[3].toInt() and 0xF0).toByte() // RCODE = 0 (no error); a query never sets RA/Z/AD/CD bits
        // ANCOUNT = 1
        header[6] = 0x00
        header[7] = 0x01

        val question = query.dnsMessage.copyOfRange(12, findQuestionEnd(query.dnsMessage))
        val answer = byteArrayOf(
            0xC0.toByte(), 0x0C, // pointer to name at offset 12
            0x00, 0x01, // TYPE A
            0x00, 0x01, // CLASS IN
            0x00, 0x00, 0x00, 0x3C, // TTL 60s
            0x00, 0x04, // RDLENGTH 4
            0x00, 0x00, 0x00, 0x00 // RDATA 0.0.0.0
        )
        val dnsResponse = header + question + answer
        return wrapUdp(query.destIp, 53, query.sourceIp, query.sourcePort, dnsResponse)
    }

    /** Wraps a real upstream DNS reply payload back into a packet addressed to the original querier. */
    fun buildForwardedResponse(query: DnsQuery, upstreamReply: ByteArray): ByteArray =
        wrapUdp(query.destIp, 53, query.sourceIp, query.sourcePort, upstreamReply)

    private fun findQuestionEnd(dns: ByteArray): Int {
        var offset = 12
        while (offset < dns.size && (dns[offset].toInt() and 0xFF) != 0) {
            offset += (dns[offset].toInt() and 0xFF) + 1
        }
        return minOf(offset + 1 + 4, dns.size) // +1 for terminator, +4 for QTYPE/QCLASS
    }

    private fun wrapUdp(
        srcIp: InetAddress, srcPort: Int, dstIp: InetAddress, dstPort: Int, payload: ByteArray
    ): ByteArray {
        val udpLength = 8 + payload.size
        val totalLength = 20 + udpLength
        val packet = ByteArray(totalLength)

        // IPv4 header
        packet[0] = 0x45 // version 4, IHL 5 (20 bytes, no options)
        packet[1] = 0x00
        writeUInt16(packet, 2, totalLength)
        packet[4] = 0x00; packet[5] = 0x00 // identification
        packet[6] = 0x40; packet[7] = 0x00 // flags: don't fragment
        packet[8] = 64 // TTL
        packet[9] = PROTOCOL_UDP.toByte()
        packet[10] = 0x00; packet[11] = 0x00 // checksum placeholder
        System.arraycopy(srcIp.address, 0, packet, 12, 4)
        System.arraycopy(dstIp.address, 0, packet, 16, 4)
        val ipChecksum = checksum(packet, 0, 20)
        writeUInt16(packet, 10, ipChecksum)

        // UDP header (checksum 0 = "not computed," which is valid for IPv4 UDP)
        writeUInt16(packet, 20, srcPort)
        writeUInt16(packet, 22, dstPort)
        writeUInt16(packet, 24, udpLength)
        writeUInt16(packet, 26, 0)

        System.arraycopy(payload, 0, packet, 28, payload.size)
        return packet
    }

    private fun readUInt16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun writeUInt16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = ((value shr 8) and 0xFF).toByte()
        data[offset + 1] = (value and 0xFF).toByte()
    }

    private fun checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        while (i < offset + length - 1) {
            sum += readUInt16(data, i)
            i += 2
        }
        if (length % 2 != 0) sum += (data[offset + length - 1].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv().toInt() and 0xFFFF
    }

    private const val PROTOCOL_UDP = 17
}
