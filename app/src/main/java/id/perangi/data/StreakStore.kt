package id.perangi.data

import android.content.Context

/**
 * Streak "hari terlindungi": dihitung sejak proteksi pertama kali aktif.
 */
object StreakStore {
    private const val PREFS = "perangi_streak"
    private const val DAY_MS = 86_400_000L

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Catat awal streak jika belum ada. Dipanggil setiap VPN start. */
    fun ensureStarted(ctx: Context) {
        val p = prefs(ctx)
        if (!p.contains("start")) {
            p.edit().putLong("start", System.currentTimeMillis()).apply()
        }
    }

    fun days(ctx: Context): Long {
        val start = prefs(ctx).getLong("start", 0L)
        if (start == 0L) return 0L
        return (System.currentTimeMillis() - start) / DAY_MS + 1
    }
}
