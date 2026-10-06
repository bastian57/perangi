package id.perangi.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import id.perangi.data.StatsRepository
import id.perangi.util.Logger
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Event loop VPN: baca paket dari TUN → filter DNS → tulis jawaban kembali.
 * Skeleton memakai blocking-read loop sederhana; Fase 1 upgrade ke pola
 * poll() seperti DNS66 (lihat FASE0_RESEARCH.md bagian 6).
 */
class PerangiVpnThread(
    private val service: VpnService,
    private val tun: ParcelFileDescriptor,
    private val stats: StatsRepository
) : Thread("PerangiVpn") {

    // TODO(Fase 1): baca upstream dari ConnectivityManager.linkProperties.dnsServers
    //              atau dari pengaturan user; opsi DoT/DoH seperti RethinkDNS.
    private val upstream: InetAddress = InetAddress.getByName("1.1.1.1")

    override fun run() {
        val input = FileInputStream(tun.fileDescriptor)
        val output = FileOutputStream(tun.fileDescriptor)
        val socket = DatagramSocket()
        // PENTING: lindungi socket upstream dari VPN agar tidak routing-loop.
        service.protect(socket)
        socket.soTimeout = 4000
        val buf = ByteArray(32767)
        Logger.d("VPN", "Thread filter berjalan")
        try {
            while (!isInterrupted) {
                val len = try {
                    input.read(buf)
                } catch (_: Exception) {
                    break // TUN ditutup
                }
                if (len <= 0) continue
                // Bungkus parsing: satu paket aneh tidak boleh membunuh thread
                // (thread mati = TUN buntu = internet lumpuh total).
                val query = try {
                    DnsPacket.parse(buf.copyOf(len))
                } catch (_: Exception) {
                    null
                } ?: continue
                val name = query.queryName ?: continue
                val dnsAnswer = if (Blocklist.isBlocked(name)) {
                    stats.recordBlocked(name)
                    Logger.d("Filter", "Diblokir: $name")
                    DnsPacket.buildNxDomainDns(query)
                } else {
                    forward(query.dnsPayload, socket) ?: continue
                }
                if (dnsAnswer.isEmpty()) continue
                try {
                    output.write(query.buildResponse(dnsAnswer))
                } catch (_: Exception) { break }
            }
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    /** Teruskan query yang lolos ke upstream via UDP. null = gagal/timeout. */
    private fun forward(dnsPayload: ByteArray, socket: DatagramSocket): ByteArray? {
        return try {
            socket.send(DatagramPacket(dnsPayload, dnsPayload.size, upstream, 53))
            val inBuf = ByteArray(4096)
            val reply = DatagramPacket(inBuf, inBuf.size)
            socket.receive(reply)
            reply.data.copyOf(reply.length)
        } catch (_: Exception) {
            null
        }
    }
}
