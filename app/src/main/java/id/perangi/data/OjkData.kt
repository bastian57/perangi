package id.perangi.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PinjolCheck(val name: String, val status: Status, val note: String) {
    enum class Status { LEGAL, ILEGAL, UNKNOWN }
}

/**
 * Database pinjol dari direktori OJK (aset lokal, dibundel per September 2026).
 * BUKAN pengganti verifikasi resmi — status izin bisa berubah,
 * selalu arahkan user verifikasi ke OJK (telp 157).
 */
object OjkData {
    private var legal: Set<String> = emptySet()
    private var ilegal: Set<String> = emptySet()
    private var warning: String = ""

    fun load(context: Context) {
        try {
            val json = JSONObject(context.assets.open("ojk_pinjol.json").bufferedReader().readText())
            warning = json.optString("_warning")
            legal = toSet(json.optJSONArray("legal"))
            ilegal = toSet(json.optJSONArray("ilegal"))
        } catch (_: Exception) { /* opsional */ }
    }

    fun check(query: String): PinjolCheck {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return PinjolCheck(query, PinjolCheck.Status.UNKNOWN, "")
        if (legal.any { it.contains(q) || q.contains(it) })
            return PinjolCheck(
                query, PinjolCheck.Status.LEGAL,
                "Terdaftar di direktori OJK (data Sep 2026). Tetap verifikasi ke OJK: telp 157."
            )
        if (ilegal.any { it.contains(q) || q.contains(it) })
            return PinjolCheck(
                query, PinjolCheck.Status.ILEGAL,
                "Izinnya DICABUT OJK — jangan pinjam, segera laporkan ke OJK 157."
            )
        return PinjolCheck(
            query, PinjolCheck.Status.UNKNOWN,
            "Tidak ada di direktori OJK — kemungkinan ILEGAL. Verifikasi ke OJK: telp 157."
        )
    }

    fun warning(): String = warning

    private fun toSet(arr: JSONArray?): Set<String> {
        if (arr == null) return emptySet()
        return (0 until arr.length()).map { arr.getString(it).lowercase() }.toSet()
    }
}
