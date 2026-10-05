package id.perangi.vpn

import android.content.Context
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

/**
 * Blocklist domain — pola DNS66 (FASE0_RESEARCH.md bagian 3):
 * HashSet + atomic swap (baca O(1) lock-free, reload tanpa blokir),
 * DITAMBAH suffix-matching karena bandar judol rajin ganti subdomain:
 * aturan "judol.com" juga memblokir "a123.judol.com".
 */
object Blocklist {

    private val blocked = AtomicReference<Set<String>>(emptySet())

    fun load(domains: Set<String>) {
        blocked.set(domains.map { normalize(it) }.filter { it.isNotEmpty() }.toSet())
    }

    /** Muat seed bawaan dari assets (dipakai sebelum update pertama dari server). */
    fun loadSeed(context: Context) {
        try {
            val text = context.assets.open("blocklist_seed.json").bufferedReader().readText()
            load(extractDomains(JSONObject(text)))
        } catch (_: Exception) { /* seed opsional */ }
    }

    fun extractDomains(json: JSONObject): Set<String> {
        val domains = mutableSetOf<String>()
        val cats = json.getJSONObject("categories")
        val keys = cats.keys()
        while (keys.hasNext()) {
            val arr = cats.getJSONArray(keys.next())
            for (i in 0 until arr.length()) domains.add(arr.getString(i))
        }
        return domains
    }

    fun isBlocked(host: String): Boolean {
        var h = normalize(host)
        val set = blocked.get()
        if (set.isEmpty()) return false
        while (true) {
            if (set.contains(h)) return true
            val dot = h.indexOf('.')
            if (dot < 0) return false
            h = h.substring(dot + 1)
        }
    }

    fun size(): Int = blocked.get().size

    private fun normalize(host: String): String =
        host.trim().trimEnd('.').lowercase()
}
