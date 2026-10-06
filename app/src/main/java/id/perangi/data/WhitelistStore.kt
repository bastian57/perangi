package id.perangi.data

import android.content.Context

/**
 * Whitelist: domain yang dikecualikan dari blokir (mis. false positive).
 * Dicek SEBELUM blocklist di PerangiVpnThread.
 */
object WhitelistStore {
    private const val PREFS = "perangi_whitelist"
    private var cache = mutableSetOf<String>()

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun init(ctx: Context) {
        cache = prefs(ctx).getStringSet("domains", emptySet())
            .orEmpty().map { it.lowercase() }.toMutableSet()
    }

    fun all(): Set<String> = cache.toSet()

    fun add(ctx: Context, domain: String) {
        val d = domain.trim().trimEnd('.').lowercase()
        if (d.isEmpty()) return
        cache.add(d)
        prefs(ctx).edit().putStringSet("domains", cache).apply()
    }

    fun remove(ctx: Context, domain: String) {
        cache.remove(domain.lowercase())
        prefs(ctx).edit().putStringSet("domains", cache).apply()
    }

    /** Suffix-match seperti Blocklist. */
    fun contains(host: String): Boolean {
        var h = host.trim().trimEnd('.').lowercase()
        while (h.isNotEmpty()) {
            if (cache.contains(h)) return true
            val dot = h.indexOf('.')
            if (dot < 0) break
            h = h.substring(dot + 1)
        }
        return false
    }
}
