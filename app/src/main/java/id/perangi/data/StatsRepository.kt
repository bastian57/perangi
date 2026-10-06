package id.perangi.data

import android.content.Context

/**
 * Statistik blokir (SharedPreferences): total + 20 domain terakhir.
 */
class StatsRepository(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("perangi_stats", Context.MODE_PRIVATE)

    fun recordBlocked(domain: String) {
        val total = prefs.getLong("blocked_total", 0) + 1
        val recent = ("${System.currentTimeMillis()}|$domain\n" + prefs.getString("recent", "").orEmpty())
            .lineSequence().take(20).joinToString("\n")
        prefs.edit()
            .putLong("blocked_total", total)
            .putString("last_blocked", domain)
            .putString("recent", recent)
            .apply()
    }

    fun totalBlocked(): Long = prefs.getLong("blocked_total", 0)

    fun lastBlocked(): String? = prefs.getString("last_blocked", null)

    fun recentBlocked(): List<Pair<Long, String>> =
        prefs.getString("recent", "").orEmpty().lineSequence()
            .filter { it.contains('|') }
            .mapNotNull { line ->
                val i = line.indexOf('|')
                val ts = line.substring(0, i).toLongOrNull() ?: return@mapNotNull null
                ts to line.substring(i + 1)
            }.toList()
}
