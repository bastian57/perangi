package id.perangi.blocklist

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import id.perangi.util.Logger
import java.util.concurrent.TimeUnit

/**
 * Update blocklist otomatis tiap 6 jam di background (Fase 1).
 * Menggantikan update-yang-hanya-saat-aplikasi-dibuka.
 */
class BlocklistUpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            BlocklistUpdater.update(applicationContext)
            Logger.d("Blocklist", "Update berkala selesai")
            Result.success()
        } catch (e: Exception) {
            Logger.e("Blocklist", "Update berkala gagal", e)
            Result.retry()
        }
    }

    companion object {
        private const val NAME = "blocklist_update"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<BlocklistUpdateWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.KEEP, req
            )
            Logger.d("Blocklist", "Jadwal update 6 jam dipasang")
        }
    }
}
