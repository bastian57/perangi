package id.perangi.blocklist

import android.content.Context
import id.perangi.vpn.Blocklist
import id.perangi.vpn.KeywordBlock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Penarik update blocklist dari server (format JSON, lihat blocklist/blocklist.json).
 * Skeleton: dipanggil saat aplikasi dibuka. Fase 1: jadikan WorkManager
 * periodik tiap 6 jam (pola DNS66: RuleDatabaseUpdateJobService).
 */
object BlocklistUpdater {

    // Blocklist resmi PERANGI (repo publik, file statis).
    private const val BLOCKLIST_URL =
        "https://raw.githubusercontent.com/bastian57/perangi/main/blocklist/blocklist.json"

    fun update(context: Context) {
        try {
            val prefs = context.getSharedPreferences("perangi", Context.MODE_PRIVATE)
            val conn = (URL(BLOCKLIST_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 15000
            }
            if (conn.responseCode != 200) return
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val version = json.getInt("version")
            if (version <= prefs.getInt("blocklist_version", 0)) return // sudah terbaru
            Blocklist.load(Blocklist.extractDomains(json))
            json.optJSONArray("keywords")?.let { arr ->
                KeywordBlock.load((0 until arr.length()).map { arr.getString(it) })
            }
            prefs.edit().putInt("blocklist_version", version).apply()
        } catch (_: Exception) {
            // Offline / URL belum diisi -> tetap pakai seed bawaan. Bukan error fatal.
        }
    }
}
