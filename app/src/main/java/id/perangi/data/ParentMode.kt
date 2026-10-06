package id.perangi.data

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

/**
 * Mode Orang Tua: kunci PIN (SHA-256 + device ID sebagai salt) untuk
 * mematikan proteksi / mengubah pengaturan sensitif.
 */
object ParentMode {
    private const val PREFS = "perangi_parent"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun salt(ctx: Context): String =
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()

    private fun hash(ctx: Context, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((pin + "|" + salt(ctx)).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean("enabled", false)

    fun setEnabled(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean("enabled", on).apply()
    }

    fun hasPin(ctx: Context): Boolean = prefs(ctx).contains("pin_hash")

    fun setPin(ctx: Context, pin: String) {
        require(pin.length in 4..6 && pin.all(Char::isDigit)) { "PIN 4-6 digit" }
        prefs(ctx).edit().putString("pin_hash", hash(ctx, pin)).apply()
    }

    fun verify(ctx: Context, pin: String): Boolean {
        val saved = prefs(ctx).getString("pin_hash", null) ?: return false
        return hash(ctx, pin) == saved
    }
}
