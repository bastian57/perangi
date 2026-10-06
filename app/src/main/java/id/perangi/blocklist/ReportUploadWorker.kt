package id.perangi.blocklist

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import id.perangi.data.DeviceId
import id.perangi.data.ReportStore
import id.perangi.data.Supabase
import org.json.JSONObject

/**
 * Upload antrean laporan lokal ke Supabase (Fase 3a).
 * No-op kalau Supabase belum dikonfigurasi — laporan tetap aman di antrean lokal.
 */
class ReportUploadWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        if (!Supabase.isConfigured()) return Result.success()
        val pending = ReportStore.pendingUpload(applicationContext)
        if (pending.isEmpty()) return Result.success()
        val hash = DeviceId.hash(applicationContext)
        val done = mutableListOf<String>()
        for (r in pending) {
            val code = try {
                Supabase.post(
                    "reports",
                    JSONObject().apply {
                        put("domain", r.domain)
                        put("category", r.category)
                        put("device_hash", hash)
                    }
                )
            } catch (_: Exception) {
                return Result.retry()
            }
            // 409 = duplikat (sudah pernah vote) → anggap selesai
            if (code in 200..299 || code == 409) done.add(r.domain)
            else return Result.retry()
        }
        ReportStore.markUploaded(applicationContext, done)
        return Result.success()
    }

    companion object {
        fun enqueue(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "upload_reports",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<ReportUploadWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
            )
        }
    }
}
