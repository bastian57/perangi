package id.perangi.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Klien REST Supabase (tanpa dependency baru — HttpURLConnection murni).
 *
 * TODO(3a): isi URL & ANON_KEY setelah project Supabase dibuat.
 * ANON_KEY dirancang publik (tertanam di semua aplikasi client) — aman di kode.
 * SERVICE_ROLE_KEY JANGAN taruh di sini — itu milik GitHub Secrets untuk robot kurasi.
 */
object Supabase {
    const val URL = "https://GANTI_DENGAN_PROJECT_URL.supabase.co"
    const val ANON_KEY = "GANTI_DENGAN_ANON_KEY"

    fun isConfigured(): Boolean =
        !URL.contains("GANTI_DENGAN") && !ANON_KEY.contains("GANTI_DENGAN")

    /** POST ke /rest/v1/<path>. Return HTTP status code. */
    fun post(path: String, body: JSONObject): Int {
        val conn = (URL("$URL/rest/v1/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("apikey", ANON_KEY)
            setRequestProperty("Authorization", "Bearer $ANON_KEY")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
            doOutput = true
        }
        return try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }
}
