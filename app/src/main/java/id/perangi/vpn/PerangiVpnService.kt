package id.perangi.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import id.perangi.data.StatsRepository
import id.perangi.data.StreakStore
import id.perangi.data.WhitelistStore
import id.perangi.data.ParentMode
import id.perangi.ui.MainActivity
import id.perangi.ui.VpnState
import id.perangi.util.Logger
/**
 * VpnService PERANGI — DNS-only VPN (arsitektur terinspirasi DNS66,
 * lihat FASE0_RESEARCH.md bagian 1). Hanya rute DNS yang masuk TUN;
 * traffic lain lewat jalur normal (hemat baterai).
 */
class PerangiVpnService : VpnService() {

    companion object {
        const val ACTION_START = "id.perangi.action.START"
        const val ACTION_STOP = "id.perangi.action.STOP"
        private const val CHANNEL_ID = "perangi_vpn"
        private const val NOTIF_ID = 1
    }

    private var tun: ParcelFileDescriptor? = null
    private var thread: PerangiVpnThread? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Watchdog: kalau thread filter mati diam-diam (TUN buntu = internet lumpuh),
     * bangun ulang VPN. Proteksi boleh mati sesaat, internet tidak boleh mati.
     */
    private val watchdog = object : Runnable {
        override fun run() {
            if (tun != null && thread?.isAlive != true) {
                Logger.e("VPN", "Watchdog: thread filter mati — membangun ulang")
                try { tun?.close() } catch (_: Exception) {}
                tun = null
                thread = null
                startVpn()
            }
            handler.postDelayed(this, 10_000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopVpn()
            else -> startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (thread?.isAlive == true) return
        // Seed lokal dulu supaya proteksi langsung jalan walau offline.
        Blocklist.loadSeed(this)
        WhitelistStore.init(this)
        StreakStore.ensureStarted(this)
        startForeground(NOTIF_ID, buildNotification())

        // Trik DNS66: TEST-NET-1 (192.0.2.0/24, RFC 5737) sebagai alamat VPN,
        // dengan alias DNS virtual 192.0.2.2 — Android mengira itu DNS server
        // dan mengirim semua query DNS ke sana = masuk ke TUN kita.
        val builder = Builder()
            .setSession("PERANGI")
            .setMtu(1500)
            .addAddress("192.0.2.1", 24)
            .addDnsServer("192.0.2.2")
            .addRoute("192.0.2.2", 32)   // HANYA rute DNS — bukan 0.0.0.0/0
            .setBlocking(true)
        // TODO(Fase 1): IPv6 ULA fd66:... + allowFamily(AF_INET6) (lihat FASE0_RESEARCH.md)
        // TODO(Fase 1): setMetered(false) untuk API 29+

        val newTun = try {
            builder.establish()
        } catch (_: Exception) { null }
        if (newTun == null) {
            stopSelf()
            return
        }
        tun = newTun
        thread = PerangiVpnThread(this, newTun, StatsRepository(this)).also { it.start() }
        VpnState.running = true
        Logger.d("VPN", "Proteksi dimulai (upstream 1.1.1.1, seed=${Blocklist.size()} domain)")
        handler.removeCallbacks(watchdog)
        handler.postDelayed(watchdog, 10_000)
    }

    private fun stopVpn() {
        handler.removeCallbacks(watchdog)
        thread?.interrupt()
        thread = null
        try { tun?.close() } catch (_: Exception) {}
        tun = null
        VpnState.running = false
        Logger.d("VPN", "Proteksi dihentikan")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        // User mencabut izin VPN dari Settings — hentikan dengan bersih.
        Logger.e("VPN", "Izin VPN dicabut sistem/user")
        if (ParentMode.isEnabled(this)) {
            alarmBocor()
        }
        stopVpn()
        super.onRevoke()
    }

    /** Alarm Mode Orang Tua: proteksi dimatikan paksa dari luar aplikasi. */
    private fun alarmBocor() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("perangi_alarm", "Alarm PERANGI", NotificationManager.IMPORTANCE_HIGH)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(this, "perangi_alarm")
            .setContentTitle("⚠️ Proteksi PERANGI dimatikan!")
            .setContentText("Seseorang mematikan proteksi dari luar aplikasi. Segera cek HP ini.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(2, n)
        Logger.e("VPN", "ALARM: proteksi dimatikan paksa (Mode Orang Tua aktif)")
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Proteksi PERANGI", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("PERANGI aktif")
            .setContentText("Situs judol & pinjol ilegal diblokir")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pi)
            .build()
    }
}
