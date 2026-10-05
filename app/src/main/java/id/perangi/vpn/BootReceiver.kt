package id.perangi.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService

/**
 * Auto-start setelah reboot — hanya jika user sebelumnya mengaktifkan
 * proteksi DAN izin VPN masih berlaku (pola DNS66: BootComplete).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("perangi", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("vpn_enabled", false)) return
        // prepare() == null artinya sudah diotorisasi sebelumnya.
        if (VpnService.prepare(context) != null) return
        context.startForegroundService(
            Intent(context, PerangiVpnService::class.java).setAction(PerangiVpnService.ACTION_START)
        )
    }
}
