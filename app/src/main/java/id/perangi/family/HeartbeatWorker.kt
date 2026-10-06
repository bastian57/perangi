package id.perangi.family

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import id.perangi.data.DeviceId
import id.perangi.data.StatsRepository
import id.perangi.data.Supabase
import id.perangi.data.UpdateChecker
import id.perangi.ui.VpnState
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Heartbeat keluarga (Fase 3b): tiap 15 menit kirim status HP anak ke Supabase.
 * Dipakai dashboard ortu + (3c) robot monitor untuk deteksi proteksi mati.
 * No-op kalau Supabase belum dikonfigurasi.
 */
class HeartbeatWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        if (!Supabase.isConfigured()) return Result.success()
        val appCtx = applicationContext
        val body = JSONObject().apply {
            put("device_hash", DeviceId.hash(appCtx))
            put("protection_on", VpnState.running)
            put("blocked_today", StatsRepository(appCtx).blockedToday())
            put("app_version", UpdateChecker.installedVersion(appCtx))
        }
        val code = try {
            Supabase.upsert("perangi_heartbeats", body)
        } catch (_: Exception) {
            return Result.retry()
        }
        return if (code in 200..299) Result.success() else Result.retry()
    }

    companion object {
        private const val NAME = "heartbeat_15m"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.KEEP, req
            )
        }
    }
}
