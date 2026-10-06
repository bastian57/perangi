package id.perangi.util

import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * Logger ringan: ring buffer di memori (max 200 baris) + Logcat.
 * Ditampilkan di dashboard agar masalah bisa didiagnosis tanpa adb.
 */
object Logger {
    private const val MAX = 200
    private val buf = ArrayDeque<String>()
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    @Synchronized
    fun d(tag: String, msg: String) {
        buf.addLast("[${fmt.format(Date())}] $tag: $msg")
        while (buf.size > MAX) buf.removeFirst()
        android.util.Log.d("PERANGI", "$tag: $msg")
    }

    @Synchronized
    fun e(tag: String, msg: String, t: Throwable? = null) {
        d(tag, "ERROR: $msg${t?.let { " (${it.message})" } ?: ""}")
    }

    @Synchronized
    fun snapshot(): List<String> = buf.toList()
}
