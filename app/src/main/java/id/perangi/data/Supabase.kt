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
    const val URL = "https://zfvkovpdkdmzxwwhtrng.supabase.co"
    const val ANON_KEY = "sb_publishable_GbWkbqe-eDDlrYwZiG8-Hg_YuYC6fW5"

    fun isConfigured(): Boolean =
        !URL.contains("GANTI_DENGAN") && !ANON_KEY.contains("GANTI_DENGAN")

    /** POST ke /rest/v1/<path>. Return HTTP status code. */
    fun post(path: String, body: JSONObject): Int =
        request("POST", path, null, body, "return=minimal")

    /** POST upsert (insert atau update bila PK bentrok). Return HTTP status code. */
    fun upsert(path: String, body: JSONObject): Int =
        request("POST", path, null, body, "resolution=merge-duplicates,return=minimal")

    /** PATCH ke /rest/v1/<path>?<query>. Return HTTP status code. */
    fun patch(path: String, query: String, body: JSONObject): Int =
        request("PATCH", path, query, body, "return=minimal")

    /** GET /rest/v1/<path>?<query>. Return body bila 200, null bila gagal. */
    fun get(path: String, query: String): String? {
        if (!isConfigured()) return null
        val conn = (URL("$URL/rest/v1/$path?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("apikey", ANON_KEY)
            setRequestProperty("Authorization", "Bearer $ANON_KEY")
        }
        return try {
            if (conn.responseCode != 200) null
            else conn.inputStream.bufferedReader().readText()
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun request(
        method: String, path: String, query: String?, body: JSONObject, prefer: String
    ): Int {
        val url = if (query == null) "$URL/rest/v1/$path" else "$URL/rest/v1/$path?$query"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("apikey", ANON_KEY)
            setRequestProperty("Authorization", "Bearer $ANON_KEY")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", prefer)
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
