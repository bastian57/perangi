package id.perangi.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import id.perangi.blocklist.BlocklistUpdater
import id.perangi.data.StatsRepository
import id.perangi.vpn.Blocklist
import id.perangi.vpn.PerangiVpnService

/**
 * Layar utama skeleton: status proteksi + tombol on/off.
 * Fase 1: ganti dengan Jetpack Compose (dashboard, kalkulator, cek pinjol,
 * mode orang tua) sesuai desain di blueprint.
 */
class MainActivity : Activity() {

    private lateinit var statusView: TextView
    private lateinit var statsView: TextView
    private lateinit var toggleButton: Button

    // startActivityForResult/onActivityResult: API klasik yang deprecated —
    // cukup untuk skeleton; Fase 1 migrasi ke Activity Result API (androidx).
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(64, 96, 64, 64)
        }
        val title = TextView(this).apply {
            text = "PERANGI"
            textSize = 40f
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Perang Melawan Judol & Pinjol Ilegal"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        statusView = TextView(this).apply {
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 16)
        }
        statsView = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        toggleButton = Button(this).apply {
            setOnClickListener { onToggleClicked() }
        }
        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(statusView)
        layout.addView(statsView)
        layout.addView(toggleButton)
        setContentView(layout)

        // Tarik update blocklist di background setiap aplikasi dibuka.
        Thread { BlocklistUpdater.update(this) }.start()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    @Suppress("DEPRECATION")
    private fun onToggleClicked() {        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            // Belum diotorisasi -> tampilkan dialog sistem Android.
            startActivityForResult(prepareIntent, REQUEST_VPN)
        } else {
            toggleVpnService()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN && resultCode == RESULT_OK) {
            startVpn()
        }
    }

    private fun toggleVpnService() {
        // Skeleton: selalu (re)start. Fase 1: cek status service yang sebenarnya.
        startVpn()
    }

    private fun startVpn() {
        startForegroundService(
            Intent(this, PerangiVpnService::class.java).setAction(PerangiVpnService.ACTION_START)
        )
        getSharedPreferences("perangi", MODE_PRIVATE).edit().putBoolean("vpn_enabled", true).apply()
        refreshUi()
    }

    private fun refreshUi() {
        val enabled = getSharedPreferences("perangi", MODE_PRIVATE).getBoolean("vpn_enabled", false)
        statusView.text = if (enabled) "🛡️ Proteksi AKTIF" else "⚠️ Proteksi MATI"
        val stats = StatsRepository(this)
        statsView.text = "Situs diblokir: ${stats.totalBlocked()}  •  " +
                "Domain di daftar: ${Blocklist.size()}" +
                (stats.lastBlocked()?.let { "\nTerakhir: $it" } ?: "")
        toggleButton.text = if (enabled) "Aktifkan Ulang Proteksi" else "Aktifkan Proteksi"
    }

    companion object {
        private const val REQUEST_VPN = 100
    }
}
