package id.perangi.data

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

/**
 * Identitas device untuk voting — berupa HASH satu arah, bukan ID mentah.
 * Privacy: server tidak pernah menerima ANDROID_ID asli.
 */
object DeviceId {
    fun hash(ctx: Context): String {
        val androidId = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ANDROID_ID
        ).orEmpty()
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("$androidId|perangi-v1".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
