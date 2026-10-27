package com.asmrone.app.data.repository

import com.asmrone.app.data.local.DownloadDao
import com.asmrone.app.data.local.DownloadEntity
import com.asmrone.app.data.local.FavoriteDao
import com.asmrone.app.data.local.FavoriteEntity
import com.asmrone.app.data.local.HistoryDao
import com.asmrone.app.data.local.PlayHistoryEntity
import com.asmrone.app.data.model.FilterItem
import com.asmrone.app.data.model.TrackNode
import com.asmrone.app.data.model.Work
import com.asmrone.app.data.model.WorksResponse
import com.asmrone.app.data.model.resolvedStreamUrl
import com.asmrone.app.data.remote.AsmrOneApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AsmrOneRepository @Inject constructor(
    private val api: AsmrOneApi,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val downloadDao: DownloadDao,
) {
    // ---- 网络 ----

    suspend fun getWorks(page: Int = 1, order: String = "create_date", sort: String = "desc"): WorksResponse =
        api.getWorks(page = page, order = order, sort = sort)

    suspend fun searchWorks(
        keyword: String,
        page: Int = 1,
        order: String = "create_date",
        sort: String = "desc",
        subtitle: Int = 0,
    ): WorksResponse =
        api.searchWorks(keyword = keyword, page = page, order = order, sort = sort, subtitle = subtitle)

    suspend fun getTracks(workId: String): List<TrackNode> =
        api.getTracks(workId = workId)

    /** 拉取字幕文本内容（.lrc / .vtt / .srt） */
    suspend fun getSubtitleText(url: String): String =
        api.fetchText(url).string()

    // ---- 筛选列表 ----

    suspend fun getTags(): List<FilterItem> = api.getTags()
    suspend fun getCircles(): List<FilterItem> = api.getCircles()
    suspend fun getVas(): List<FilterItem> = api.getVas()

    /** 展平文件树，返回所有音频文件节点（真实 API type 为 "audio"，另有 image/text/folder） */
    fun flattenTracks(nodes: List<TrackNode>): List<TrackNode> {
        val result = mutableListOf<TrackNode>()
        fun walk(list: List<TrackNode>) {
            for (node in list) {
                if (node.isAudio) result.add(node)
                if (node.children.isNotEmpty()) walk(node.children)
            }
        }
        walk(nodes)
        return result
    }

    // ---- 收藏 ----

    fun observeFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.observeAll()
    fun observeIsFavorite(workId: Int): Flow<Boolean> = favoriteDao.observeIsFavorite(workId)

    suspend fun toggleFavorite(work: Work) {
        val id = work.id ?: return
        val existing = favoriteDao.getById(id)
        if (existing != null) {
            favoriteDao.deleteById(id)
        } else {
            favoriteDao.insert(work.toFavoriteEntity())
        }
    }

    suspend fun isFavorite(workId: Int): Boolean = favoriteDao.getById(workId) != null

    /** 直接取消收藏（收藏页红心操作） */
    suspend fun removeFavorite(workId: Int) = favoriteDao.deleteById(workId)

    // ---- 历史 ----

    fun observeHistory(): Flow<List<PlayHistoryEntity>> = historyDao.observeAll()

    suspend fun addHistory(work: Work, track: TrackNode? = null) {
        val id = work.id ?: return
        historyDao.insert(
            PlayHistoryEntity(
                workId = id,
                title = work.title ?: "",
                circleName = work.circleName,
                vaNames = work.vaNames.joinToString(" / "),
                coverUrl = work.coverUrl,
                rjCode = work.rjCode,
                trackTitle = track?.title,
                trackUrl = track?.resolvedStreamUrl,
            )
        )
    }

    suspend fun clearHistory() = historyDao.clearAll()

    // ---- 下载 ----

    fun observeDownloads(): Flow<List<DownloadEntity>> = downloadDao.observeAll()
    suspend fun getDownloadByUrl(url: String): DownloadEntity? = downloadDao.getByUrl(url)
    suspend fun addDownload(entity: DownloadEntity) = downloadDao.insert(entity)
    suspend fun removeDownload(url: String) = downloadDao.deleteByUrl(url)
}

/** Work -> FavoriteEntity 映射 */
fun Work.toFavoriteEntity(): FavoriteEntity = FavoriteEntity(
    workId = id ?: 0,
    title = title ?: "",
    circleName = circleName,
    vaNames = vaNames.joinToString(" / "),
    coverUrl = coverUrl,
    rjCode = rjCode,
    rating = rating,
    duration = duration ?: 0,
    tags = tags.joinToString(" / ") { it.displayName },
)
