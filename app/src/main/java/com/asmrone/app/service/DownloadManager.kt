package com.asmrone.app.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.asmrone.app.data.model.TrackNode
import com.asmrone.app.data.model.Work
import com.asmrone.app.data.model.resolvedDownloadUrl
import com.asmrone.app.data.model.resolvedStreamUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 下载管理器：负责调度后台下载任务。
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    fun download(work: Work, track: TrackNode) {
        val url = track.resolvedDownloadUrl ?: track.resolvedStreamUrl ?: return
        val workId = work.id ?: return

        val inputData = Data.Builder()
            .putString(DownloadWorker.KEY_URL, url)
            .putInt(DownloadWorker.KEY_WORK_ID, workId)
            .putString(DownloadWorker.KEY_WORK_TITLE, work.title ?: "")
            .putString(DownloadWorker.KEY_TRACK_TITLE, track.title ?: "")
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("download_$url")
            .build()

        workManager.enqueue(request)
    }
}
