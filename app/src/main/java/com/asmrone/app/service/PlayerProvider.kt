package com.asmrone.app.service

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.asmrone.app.data.model.TrackNode
import com.asmrone.app.data.model.Work
import com.asmrone.app.data.model.resolvedDownloadUrl
import com.asmrone.app.data.model.resolvedStreamUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 共享的 ExoPlayer 实例提供者（单例）。
 */
@Singleton
class PlayerProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val player: ExoPlayer by lazy { ExoPlayer.Builder(context).build() }
}

/**
 * 把作品与音轨转换成播放项列表。
 */
object MediaItemFactory {

    fun buildPlaylist(work: Work, tracks: List<TrackNode>): List<MediaItem> =
        tracks.map { track -> buildMediaItem(work, track) }

    fun buildMediaItem(work: Work, track: TrackNode): MediaItem {
        val url = track.resolvedStreamUrl ?: track.resolvedDownloadUrl ?: ""
        return MediaItem.Builder()
            .setUri(url)
            .setMediaId(track.hash ?: url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(work.circleName)
                    .setAlbumTitle(work.title)
                    .setArtworkUri(work.coverUrl?.let(Uri::parse))
                    .build()
            )
            .build()
    }
}
