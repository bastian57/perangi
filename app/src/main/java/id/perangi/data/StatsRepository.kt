package id.perangi.data

import android.content.Context

/**
 * Statistik blokir sederhana (SharedPreferences).
 * Fase 1: tampilkan di dashboard ("minggu ini X situs diblokir").
 */
class StatsRepository(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("perangi_stats", Context.MODE_PRIVATE)

    fun recordBlocked(domain: String) {
        prefs.edit()
            .putLong("blocked_total", prefs.getLong("blocked_total", 0) + 1)
            .putString("last_blocked", domain)
            .apply()
        // TODO(Fase 1): agregasi per-domain untuk "top domain diblokir".
    }

    fun totalBlocked(): Long = prefs.getLong("blocked_total", 0)

    fun lastBlocked(): String? = prefs.getString("last_blocked", null)
}
