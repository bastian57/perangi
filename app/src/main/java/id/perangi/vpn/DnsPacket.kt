package id.perangi.vpn

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Query DNS yang diekstrak dari paket IP yang masuk lewat TUN.
 */
data class DnsQuery(
    val srcAddr: ByteArray,
    val dstAddr: ByteArray,
    val srcPort: Int,
    val dstPort: Int,
    val dnsPayload: ByteArray
) {
    /** Nama domain dari section QUESTION (lowercase, tanpa titik akhir). null bila tak valid. */
    val queryName: String?
        get() = DnsPacket.extractQueryName(dnsPayload)

    /** Bungkus jawaban DNS menjadi paket IP+UDP lengkap (alamat & port dibalik). */
    fun buildResponse(dnsAnswer: ByteArray): ByteArray =
        DnsPacket.buildIpUdpResponse(this, dnsAnswer)
}

/**
 * Parser & pembangun paket DNS minimalis — implementasi clean-room.
 * Arsitektur terinspirasi DNS66 (lihat FASE0_RESEARCH.md), kode ditulis dari nol.
 * Hanya menangani IPv4 + UDP + query standar — sudah cukup untuk filtering.
 */
object DnsPacket {

    /** Ekstrak DnsQuery dari paket IP mentah. null = bukan paket DNS (abaikan). */
    fun parse(packet: ByteArray): DnsQuery? {
        if (packet.size < 28) return null
        if ((packet[0].toInt() shr 4) != 4) return null          // bukan IPv4
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20 || packet.size < ihl + 8) return null
        if (packet[9] != 17.toByte()) return null                // bukan UDP
        val bb = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        val srcPort = bb.getShort(ihl).toInt() and 0xFFFF
        val dstPort = bb.getShort(ihl + 2).toInt() and 0xFFFF
        if (dstPort != 53) return null                           // bukan DNS
        val dnsStart = ihl + 8
        if (dnsStart + 12 > packet.size) return null
        return DnsQuery(
            srcAddr = packet.copyOfRange(12, 16),
            dstAddr = packet.copyOfRange(16, 20),
            srcPort = srcPort,
            dstPort = dstPort,
            dnsPayload = packet.copyOfRange(dnsStart, packet.size)
        )
    }

    /** Ambil nama domain dari QUESTION section. */
    fun extractQueryName(dns: ByteArray): String? {
        if (dns.size < 12) return null
        val qdCount = ByteBuffer.wrap(dns).order(ByteOrder.BIG_ENDIAN).getShort(4).toInt() and 0xFFFF
        if (qdCount < 1) return null
        val sb = StringBuilder()
        var pos = 12
        while (true) {
            if (pos >= dns.size) return null
            val len = dns[pos].toInt() and 0xFF
            if (len == 0) { pos++; break }
            if (len and 0xC0 != 0) return null                   // pointer tak valid di question
            if (pos + 1 + len > dns.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(dns, pos + 1, len, Charsets.US_ASCII))
            pos += 1 + len
        }
        if (pos + 4 > dns.size) return null                      // QTYPE + QCLASS
        if (sb.isEmpty()) return null
        return sb.toString().lowercase()
    }

    /**
     * Bangun pesan DNS jawaban NXDOMAIN dari query asli:
     * flags 0x8183 (response, RD+RA, RCODE=3), question disalin verbatim.
     */
    fun buildNxDomainDns(query: DnsQuery): ByteArray {
        val q = query.dnsPayload
        val qEnd = questionEnd(q) ?: return ByteArray(0)
        val questionLen = qEnd - 12
        val qd = ByteBuffer.wrap(q).order(ByteOrder.BIG_ENDIAN)
        val out = ByteBuffer.allocate(12 + questionLen).order(ByteOrder.BIG_ENDIAN)
        out.putShort(qd.getShort(0))          // ID sama dengan query
        out.putShort(0x8183.toShort())       // QR=1, RD=1, RA=1, RCODE=3 (NXDOMAIN)
        out.putShort(qd.getShort(4))          // QDCOUNT
        out.putShort(0)                      // ANCOUNT
        out.putShort(0)                      // NSCOUNT
        out.putShort(0)                      // ARCOUNT
        out.put(q, 12, questionLen)          // salin question section
        return out.array()
    }

    /** Bungkus payload DNS menjadi paket IPv4+UDP dengan src/dst dibalik. */
    fun buildIpUdpResponse(query: DnsQuery, dnsAnswer: ByteArray): ByteArray {
        val totalLen = 20 + 8 + dnsAnswer.size
        val out = ByteBuffer.allocate(totalLen).order(ByteOrder.BIG_ENDIAN)
        // --- header IPv4 (20 byte, tanpa opsi) ---
        out.put(0x45.toByte())               // version=4, IHL=5
        out.put(0)                            // TOS
        out.putShort(totalLen.toShort())
        out.putShort(0)                       // identification
        out.putShort(0)                       // flags + fragment offset
        out.put(64.toByte())                  // TTL
        out.put(17.toByte())                  // protocol = UDP
        out.putShort(0)                       // checksum (diisi di bawah)
        out.put(query.dstAddr)                // src = dst paket asli
        out.put(query.srcAddr)                // dst = src paket asli
        out.putShort(10, ipChecksum(out.array(), 0, 20).toShort())
        // --- header UDP (8 byte) ---
        out.putShort(query.dstPort.toShort()) // src port = dst port asli
        out.putShort(query.srcPort.toShort()) // dst port = src port asli
        out.putShort((8 + dnsAnswer.size).toShort())
        out.putShort(0)                       // checksum UDP = 0 (opsional di IPv4)
        // --- payload DNS ---
        out.put(dnsAnswer)
        return out.array()
    }

    private fun questionEnd(dns: ByteArray): Int? {
        var pos = 12
        while (true) {
            if (pos >= dns.size) return null
            val len = dns[pos].toInt() and 0xFF
            if (len == 0) { pos++; break }
            if (len and 0xC0 != 0 || pos + 1 + len > dns.size) return null
            pos += 1 + len
        }
        return if (pos + 4 <= dns.size) pos + 4 else null
    }

    private fun ipChecksum(data: ByteArray, offset: Int, len: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + len) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            if (sum > 0xFFFF) sum = (sum and 0xFFFF) + 1
            i += 2
        }
        return sum.inv() and 0xFFFF
    }
}
