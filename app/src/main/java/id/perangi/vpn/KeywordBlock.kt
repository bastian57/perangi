package id.perangi.vpn

/**
 * Blokir pola kata (substring pada hostname).
 *
 * Bandar judol memutar domain agresif, tapi polanya khas Indonesia:
 * "togel", "gacor", "maxwin", dsb. Hampir tidak ada domain legit yang
 * mengandung pola ini — false positive ditangani Whitelist (dicek duluan).
 *
 * Daftar bisa diupdate jarak jauh via blocklist.json ("keywords").
 */
object KeywordBlock {

    /** Seed bawaan — dipakai sebelum update pertama dari server. */
    val DEFAULT_KEYWORDS = listOf(
        "togel",
        "gacor",
        "maxwin",
        "judol",
        "judionline",
        "slotgacor",
        "gacorslot",
        "rtpslot",
        "polaslot",
        "infoslot",
        "jamgacor",
        "gatesofolympus",
        "starlightprincess",
        "sweetbonanza",
        "mahjongways"
    )

    @Volatile
    private var keywords: List<String> = DEFAULT_KEYWORDS

    fun load(list: List<String>) {
        val clean = list.map { it.trim().lowercase() }.filter { it.length >= 3 }
        if (clean.isNotEmpty()) keywords = clean
    }

    fun get(): List<String> = keywords

    fun matches(host: String): Boolean {
        val h = host.trim().lowercase()
        if (h.isEmpty()) return false
        return keywords.any { h.contains(it) }
    }
}
