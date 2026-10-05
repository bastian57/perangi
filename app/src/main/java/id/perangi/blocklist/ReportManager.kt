package id.perangi.blocklist

import android.content.Context

/**
 * Sistem laporan komunitas (Opsi B — lihat blueprint).
 * Fase 3: kirim {domain, kategori, install_id} ke backend; backend melakukan
 * auto-cek + voting threshold sebelum masuk blocklist. Anti-abuse: 1 HP = 1 suara.
 *
 * Skeleton: laporan disimpan lokal dulu (antrean), dikirim saat backend siap.
 */
object ReportManager {

    fun reportDomain(context: Context, domain: String, category: String) {
        val prefs = context.getSharedPreferences("perangi_reports", Context.MODE_PRIVATE)
        val queue = prefs.getStringSet("queue", emptySet())!!.toMutableSet()
        queue.add("${System.currentTimeMillis()}|$category|$domain")
        // Batasi antrean lokal: max 50 laporan (anti-spam sederhana).
        prefs.edit().putStringSet("queue", queue.takeLast(50).toSet()).apply()
        // TODO(Fase 3): sinkronkan antrean ke backend saat online.
    }

    fun pendingCount(context: Context): Int {
        val prefs = context.getSharedPreferences("perangi_reports", Context.MODE_PRIVATE)
        return prefs.getStringSet("queue", emptySet())!!.size
    }
}
