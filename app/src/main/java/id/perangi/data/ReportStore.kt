package id.perangi.data

import android.content.Context
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Report(val domain: String, val category: String, val time: Long)

val REPORT_CATEGORIES = listOf("Judol", "Pinjol Ilegal", "Iklan Judi", "Lainnya")

/**
 * Antrean laporan komunitas v1 (lokal dulu — kurasi manual).
 * Anti-abuse: 1 device 1 suara per domain, maksimal 5 laporan/hari.
 */
object ReportStore {
    private const val PREFS = "perangi_reports"
    private const val MAX_PER_DAY = 5

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun deviceId(ctx: Context): String =
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun reportsToday(ctx: Context): Int =
        prefs(ctx).getInt("count_" + todayKey(), 0)

    fun canReport(ctx: Context): Boolean = reportsToday(ctx) < MAX_PER_DAY

    fun hasVoted(ctx: Context, domain: String): Boolean {
        val voted = prefs(ctx).getStringSet("voted", emptySet()).orEmpty()
        return voted.contains(normalize(domain))
    }

    /** @return null jika berhasil, pesan error jika gagal. */
    fun submit(ctx: Context, domain: String, category: String): String? {
        val d = normalize(domain)
        if (!d.matches(Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+\$"))) {
            return "Format domain tidak valid (contoh: situsjudol.com)"
        }
        if (hasVoted(ctx, d)) return "Domain ini sudah kamu laporkan."
        if (!canReport(ctx)) return "Batas $MAX_PER_DAY laporan/hari tercapai. Coba lagi besok."
        val p = prefs(ctx)
        val arr = JSONArray(p.getString("queue", "[]"))
        arr.put(JSONObject().apply {
            put("domain", d)
            put("category", category)
            put("time", System.currentTimeMillis())
        })
        val voted = p.getStringSet("voted", emptySet()).orEmpty().toMutableSet()
        voted.add(d)
        p.edit()
            .putString("queue", arr.toString())
            .putStringSet("voted", voted)
            .putInt("count_" + todayKey(), reportsToday(ctx) + 1)
            .apply()
        return null
    }

    fun myReports(ctx: Context): List<Report> {
        val arr = try {
            JSONArray(prefs(ctx).getString("queue", "[]"))
        } catch (_: Exception) {
            return emptyList()
        }
        return (0 until arr.length()).mapNotNull {
            try {
                val o = arr.getJSONObject(it)
                Report(o.getString("domain"), o.getString("category"), o.getLong("time"))
            } catch (_: Exception) {
                null
            }
        }.reversed()
    }

    private fun normalize(d: String) = d.trim().trimEnd('.').lowercase()
}
