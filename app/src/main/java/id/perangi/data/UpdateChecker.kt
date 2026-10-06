package id.perangi.data

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

data class ReleaseInfo(
    val tag: String,
    val name: String,
    val publishedAtMs: Long,
    val notes: String,
    val apkUrl: String
)

/**
 * Cek pembaruan via GitHub Releases (rolling tag "latest").
 * Tanpa login, tanpa server sendiri — memakai API publik GitHub.
 */
object UpdateChecker {
    private const val API = "https://api.github.com/repos/bastian57/perangi/releases/latest"

    fun installedVersion(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    } catch (_: Exception) {
        "?"
    }

    /** Waktu APK dipasang/diupdate di HP — pembanding "ada yang lebih baru?". */
    fun installedTime(ctx: Context): Long = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).lastUpdateTime
    } catch (_: Exception) {
        0L
    }

    /** null = gagal (offline / release belum ada). */
    fun check(): ReleaseInfo? {
        return try {
            val conn = (URL(API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (conn.responseCode != 200) return null
            val j = JSONObject(conn.inputStream.bufferedReader().readText())
            var apkUrl = ""
            j.optJSONArray("assets")?.let { assets ->
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk")) {
                        apkUrl = a.optString("browser_download_url")
                        break
                    }
                }
            }
            val ms = try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    .parse(j.optString("published_at"))?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
            ReleaseInfo(
                tag = j.optString("tag_name"),
                name = j.optString("name"),
                publishedAtMs = ms,
                notes = j.optString("body"),
                apkUrl = apkUrl
            )
        } catch (_: Exception) {
            null
        }
    }
}
