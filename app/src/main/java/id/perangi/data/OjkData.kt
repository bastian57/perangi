package id.perangi.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PinjolCheck(val name: String, val status: Status, val note: String) {
    enum class Status { LEGAL, ILEGAL, UNKNOWN }
}

/**
 * Database contoh pinjol (aset lokal). BUKAN database resmi OJK —
 * selalu arahkan user verifikasi ke OJK (telp 157). Database penuh
 * menyusul di Fase 2/3 bersama backend laporan komunitas.
 */
object OjkData {
    private var legal: Set<String> = emptySet()
    private var ilegal: Set<String> = emptySet()
    private var warning: String = ""

    fun load(context: Context) {
        try {
            val json = JSONObject(context.assets.open("ojk_sample.json").bufferedReader().readText())
            warning = json.optString("_warning")
            legal = toSet(json.optJSONArray("legal_contoh"))
            ilegal = toSet(json.optJSONArray("ilegal_contoh"))
        } catch (_: Exception) { /* opsional */ }
    }

    fun check(query: String): PinjolCheck {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return PinjolCheck(query, PinjolCheck.Status.UNKNOWN, "")
        if (legal.any { it.contains(q) || q.contains(it) })
            return PinjolCheck(query, PinjolCheck.Status.LEGAL, "Ada di database contoh sebagai terdaftar OJK.")
        if (ilegal.any { it.contains(q) || q.contains(it) })
            return PinjolCheck(query, PinjolCheck.Status.ILEGAL, "Terindikasi ILEGAL — jangan pinjam, segera laporkan.")
        return PinjolCheck(
            query, PinjolCheck.Status.UNKNOWN,
            "Tidak ada di database contoh. Verifikasi langsung ke OJK: telp 157."
        )
    }

    fun warning(): String = warning

    private fun toSet(arr: JSONArray?): Set<String> {
        if (arr == null) return emptySet()
        return (0 until arr.length()).map { arr.getString(it).lowercase() }.toSet()
    }
}
