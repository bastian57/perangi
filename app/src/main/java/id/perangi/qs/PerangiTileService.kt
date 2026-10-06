package id.perangi.qs

import android.content.Intent
import android.net.VpnService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import id.perangi.ui.MainActivity
import id.perangi.ui.VpnState
import id.perangi.util.Logger
import id.perangi.vpn.PerangiVpnService

/**
 * Quick Settings Tile: toggle proteksi 1-tap dari notification shade.
 * TileService jalan di proses yang sama, jadi bisa baca VpnState langsung.
 */
class PerangiTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        unlockAndRun {
            if (VpnState.running) {
                startForegroundService(
                    Intent(this, PerangiVpnService::class.java)
                        .setAction(PerangiVpnService.ACTION_STOP)
                )
                getSharedPreferences("perangi", MODE_PRIVATE)
                    .edit().putBoolean("vpn_enabled", false).apply()
                Logger.d("Tile", "Proteksi dimatikan dari Quick Settings")
            } else {
                if (VpnService.prepare(this) == null) {
                    startForegroundService(
                        Intent(this, PerangiVpnService::class.java)
                            .setAction(PerangiVpnService.ACTION_START)
                    )
                    getSharedPreferences("perangi", MODE_PRIVATE)
                        .edit().putBoolean("vpn_enabled", true).apply()
                    Logger.d("Tile", "Proteksi dinyalakan dari Quick Settings")
                } else {
                    // Izin VPN belum ada — buka aplikasi untuk alur onboarding.
                    startActivityAndCollapse(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            refresh()
        }
    }

    private fun refresh() {
        qsTile?.let {
            it.state = if (VpnState.running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            it.label = "PERANGI"
            it.updateTile()
        }
    }
}
