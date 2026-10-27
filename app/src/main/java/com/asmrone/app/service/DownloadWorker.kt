package com.asmrone.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.asmrone.app.R
import com.asmrone.app.data.local.DownloadDao
import com.asmrone.app.data.local.DownloadEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * 下载单个音轨的后台任务：下载到应用私有目录，完成后入库并发通知。
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val workId = inputData.getInt(KEY_WORK_ID, 0)
        val workTitle = inputData.getString(KEY_WORK_TITLE) ?: ""
        val trackTitle = inputData.getString(KEY_TRACK_TITLE) ?: ""

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AsmrOne/1.0 (Android)")
                .header("Referer", "https://www.asmr.one/")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.retry()
                val body = response.body ?: return@withContext Result.retry()

                val dir = File(applicationContext.filesDir, "downloads")
                if (!dir.exists()) dir.mkdirs()

                // 文件名：作品号_音轨名，去除非法字符
                val safeName = trackTitle
                    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    .take(80)
                val ext = inferExtension(url, response.header("Content-Type"))
                val file = File(dir, "${workId}_${safeName}.$ext")

                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                downloadDao.insert(
                    DownloadEntity(
                        trackUrl = url,
                        workId = workId,
                        workTitle = workTitle,
                        trackTitle = trackTitle,
                        localPath = file.absolutePath,
                        fileSize = file.length(),
                    )
                )
            }
            notifyComplete(trackTitle)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /** 下载完成系统通知（未授权通知权限时静默跳过） */
    private fun notifyComplete(trackTitle: String) {
        runCatching {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "下载", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("下载完成")
                .setContentText(trackTitle)
                .setAutoCancel(true)
                .build()
            androidx.core.app.NotificationManagerCompat.from(applicationContext)
                .notify(inputData.hashCode(), notification)
        }
    }

    private fun inferExtension(url: String, contentType: String?): String {
        contentType?.let {
            return when {
                it.contains("mpeg") -> "mp3"
                it.contains("flac") -> "flac"
                it.contains("wav") -> "wav"
                it.contains("mp4") || it.contains("m4a") -> "m4a"
                it.contains("ogg") -> "ogg"
                else -> "mp3"
            }
        }
        val path = url.substringBefore('?')
        return path.substringAfterLast('.', "mp3")
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_WORK_ID = "work_id"
        const val KEY_WORK_TITLE = "work_title"
        const val KEY_TRACK_TITLE = "track_title"
        private const val CHANNEL_ID = "asmr_download"
    }
}
