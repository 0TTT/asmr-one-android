package com.asmrone.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本地收藏的作品。
 */
@Entity(tableName = "favorite_works")
data class FavoriteEntity(
    @PrimaryKey val workId: Int,
    val title: String,
    val circleName: String,
    val vaNames: String,
    val coverUrl: String?,
    val rjCode: String,
    val rating: Float,
    val duration: Int,
    val tags: String,
    val addedAt: Long = System.currentTimeMillis(),
)

/**
 * 播放历史记录。
 */
@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workId: Int,
    val title: String,
    val circleName: String,
    val vaNames: String,
    val coverUrl: String?,
    val rjCode: String,
    val trackTitle: String?,
    val trackUrl: String?,
    val listenedAt: Long = System.currentTimeMillis(),
)

/**
 * 下载记录（音频文件离线缓存）。
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val trackUrl: String,
    val workId: Int,
    val workTitle: String,
    val trackTitle: String,
    val localPath: String,
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
)
