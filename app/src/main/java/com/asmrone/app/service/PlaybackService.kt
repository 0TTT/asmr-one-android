package com.asmrone.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.Coil
import com.asmrone.app.MainActivity
import com.asmrone.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 前台播放服务，基于 Media3 MediaSessionService + ExoPlayer。
 * 支持后台播放、锁屏/通知栏控制。
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playerProvider: PlayerProvider

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = playerProvider.player

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player.setAudioAttributes(audioAttributes, true)
        player.setHandleAudioBecomingNoisy(true)

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setBitmapLoader(CoilBitmapLoader(this, Coil.imageLoader(this)))
            .build()

        // 关键修复：startForegroundService 启动后必须在 5 秒内调用 startForeground，
        // 否则触发 ForegroundServiceDidNotStartInTimeException 崩溃。
        // Media3 的 startForeground 依赖 player 状态变化，而首次播放时 player 状态
        // 在 session 挂载监听前就已变化，可能不触发 —— 这里主动进入前台兜底。
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildInitialNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "播放",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "播放中的通知"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildInitialNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AsmrOne")
            .setContentText("正在播放")
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        // 注意：不在此处 release player，因为它是 PlayerProvider 持有的单例，
        // 由应用进程结束时统一释放。
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "asmr_playback"
        private const val NOTIFICATION_ID = 1001
    }
}
