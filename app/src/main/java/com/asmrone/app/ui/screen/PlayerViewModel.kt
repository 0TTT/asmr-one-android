package com.asmrone.app.ui.screen

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.asmrone.app.data.model.SubtitleLine
import com.asmrone.app.data.model.TrackNode
import com.asmrone.app.data.model.Work
import com.asmrone.app.data.model.normalizeSubtitleBaseName
import com.asmrone.app.data.model.parseSubtitle
import com.asmrone.app.data.repository.AsmrOneRepository
import com.asmrone.app.service.MediaItemFactory
import com.asmrone.app.service.PlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 播放队列条目 */
data class QueueEntry(
    val index: Int,
    val title: String,
    val subtitle: String,
)

/** 播放队列状态 */
data class QueueState(
    val entries: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
)

/**
 * 播放控制 ViewModel。
 * 持有 ExoPlayer 引用，暴露播放状态给 UI。
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerProvider: com.asmrone.app.service.PlayerProvider,
    private val repository: AsmrOneRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val player: ExoPlayer = playerProvider.player

    private fun startPlaybackService() {
        context.startForegroundService(Intent(context, PlaybackService::class.java))
    }

    private val _nowPlaying = MutableStateFlow<Pair<Work, TrackNode>?>(null)
    val nowPlaying: StateFlow<Pair<Work, TrackNode>?> = _nowPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** 播放进度（毫秒），500ms 轮询刷新 */
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    /** 当前音轨总时长（毫秒），未知为 0 */
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    /** 播放队列（供全屏播放页展示） */
    private val _queue = MutableStateFlow(QueueState())
    val queue: StateFlow<QueueState> = _queue.asStateFlow()

    /** 循环模式：Player.REPEAT_MODE_OFF / ALL / ONE */
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    /** 倍速 */
    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _sleepTimerActive = MutableStateFlow(false)
    val sleepTimerActive: StateFlow<Boolean> = _sleepTimerActive.asStateFlow()

    /** 当前音轨的字幕行（已按时间排序）；空列表表示无字幕 */
    private val _subtitleLines = MutableStateFlow<List<SubtitleLine>>(emptyList())
    val subtitleLines: StateFlow<List<SubtitleLine>> = _subtitleLines.asStateFlow()

    /** 当前作品全部字幕文件（供手动匹配选择） */
    private val _subtitleCandidates = MutableStateFlow<List<TrackNode>>(emptyList())
    val subtitleCandidates: StateFlow<List<TrackNode>> = _subtitleCandidates.asStateFlow()

    /** 当前音轨生效的字幕 URL（自动匹配或手动选择），用于选择器高亮 */
    private val _currentSubtitleUrl = MutableStateFlow<String?>(null)
    val currentSubtitleUrl: StateFlow<String?> = _currentSubtitleUrl.asStateFlow()

    /** 当前作品的字幕映射：音频 hash -> 字幕文件 URL（自动匹配） */
    private var subtitleMap: Map<String, String> = emptyMap()

    /** 用户手动指定的映射（优先于自动匹配）：音频 hash -> 字幕 URL */
    private val manualSubtitles = mutableMapOf<String, String>()

    private var playlistWork: Work? = null
    private var playlistTracks: List<TrackNode> = emptyList()
    private var sleepJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // 自动连播 / 手动跳转 / 队列变更时统一同步
            syncNowPlayingToCurrent()
            refreshQueue()
            loadCurrentSubtitle()
        }
    }

    init {
        player.addListener(listener)
        viewModelScope.launch {
            while (isActive) {
                _positionMs.value = player.currentPosition.coerceAtLeast(0L)
                _durationMs.value = player.duration.takeIf { it > 0L } ?: 0L
                delay(500)
            }
        }
    }

    /** 换曲（含自动连播/手动跳转）时同步 nowPlaying 并写播放历史 */
    private fun syncNowPlayingToCurrent() {
        val work = playlistWork ?: return
        if (playlistTracks.isEmpty()) return
        val track = playlistTracks.getOrNull(player.currentMediaItemIndex) ?: return
        _nowPlaying.value = work to track
        viewModelScope.launch { repository.addHistory(work, track) }
    }

    private fun refreshQueue() {
        val entries = (0 until player.mediaItemCount).map { i ->
            val meta = player.getMediaItemAt(i).mediaMetadata
            QueueEntry(
                index = i,
                title = meta.title?.toString().orEmpty(),
                subtitle = meta.albumTitle?.toString().orEmpty(),
            )
        }
        _queue.value = QueueState(entries, player.currentMediaItemIndex)
    }

    /** 播放单个音轨 */
    fun play(work: Work, track: TrackNode) {
        startPlaybackService()
        playlistWork = work
        playlistTracks = listOf(track)
        _nowPlaying.value = work to track
        _subtitleLines.value = emptyList()
        _subtitleCandidates.value = emptyList()
        manualSubtitles.clear()
        player.setMediaItem(MediaItemFactory.buildMediaItem(work, track))
        player.prepare()
        player.playWhenReady = true
        refreshQueue()
        loadSubtitleMap(work)
    }

    /** 播放整个作品（音轨列表作为播放队列） */
    fun playAll(work: Work, tracks: List<TrackNode>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        startPlaybackService()
        playlistWork = work
        playlistTracks = tracks
        _nowPlaying.value = work to tracks[startIndex]
        _subtitleLines.value = emptyList()
        _subtitleCandidates.value = emptyList()
        manualSubtitles.clear()
        player.setMediaItems(MediaItemFactory.buildPlaylist(work, tracks), startIndex, 0L)
        player.prepare()
        player.playWhenReady = true
        refreshQueue()
        loadSubtitleMap(work)
    }

    /**
     * 拉取作品文件树并构建 音频->字幕 映射，随后加载当前音轨的字幕。
     * 字幕与音频在同一目录下、文件名相同或接近（如 Foo.mp3 / Foo.lrc / Foo_SEなし.lrc）。
     */
    private fun loadSubtitleMap(work: Work) {
        val id = work.id ?: return
        viewModelScope.launch {
            try {
                val nodes = repository.getTracks(id.toString())
                subtitleMap = buildSubtitleMap(nodes)
                _subtitleCandidates.value = collectSubtitles(nodes)
            } catch (_: Exception) {
                subtitleMap = emptyMap()
                _subtitleCandidates.value = emptyList()
            }
            loadCurrentSubtitle()
        }
    }

    private fun collectSubtitles(nodes: List<TrackNode>): List<TrackNode> {
        val out = mutableListOf<TrackNode>()
        fun walk(list: List<TrackNode>) {
            for (node in list) {
                if (node.isSubtitle) out.add(node)
                if (node.children.isNotEmpty()) walk(node.children)
            }
        }
        walk(nodes)
        return out
    }

    private fun buildSubtitleMap(nodes: List<TrackNode>): Map<String, String> {
        // 全树字幕池：字幕与音频可能不在同一层（如 .lrc 在「音声（MP3）」层，
        // 而 _SEなし 音频在「音声（MP3）/SEなし」子层），因此需全局匹配。
        val allSubs = collectSubtitles(nodes)
        val map = mutableMapOf<String, String>()
        fun walk(list: List<TrackNode>) {
            for (audio in list.filter { it.isAudio }) {
                val hash = audio.hash ?: continue
                val sub = matchSubtitle(audio, allSubs) ?: continue
                val url = sub.mediaStreamUrl ?: continue
                if (hash !in map) map[hash] = url
            }
            for (node in list) if (node.children.isNotEmpty()) walk(node.children)
        }
        walk(nodes)
        return map
    }

    /**
     * 字幕-音频模糊匹配：规范化后相等优先；其次互为前缀（公共部分越长越接近）。
     * 典型场景："01_xxx.mp3" 与 "01_xxx.lrc" 或 "01_xxx_SEなし.lrc"。
     */
    private fun matchSubtitle(audio: TrackNode, subs: List<TrackNode>): TrackNode? {
        val aNorm = normalizeSubtitleBaseName(audio.title?.substringBeforeLast('.') ?: "")
        if (aNorm.isEmpty()) return null
        subs.firstOrNull { normalizeSubtitleBaseName(it.title?.substringBeforeLast('.') ?: "") == aNorm }
            ?.let { return it }
        return subs.mapNotNull { s ->
            val sNorm = normalizeSubtitleBaseName(s.title?.substringBeforeLast('.') ?: "")
            if (sNorm.isEmpty()) null
            else if (sNorm.startsWith(aNorm) || aNorm.startsWith(sNorm)) s to minOf(sNorm.length, aNorm.length)
            else null
        }.maxByOrNull { it.second }?.first
    }

    /** 加载当前音轨对应的字幕内容并解析（手动选择优先于自动匹配；无字幕则清空） */
    private fun loadCurrentSubtitle() {
        val track = playlistTracks.getOrNull(player.currentMediaItemIndex)
        val hash = track?.hash
        val url = hash?.let { manualSubtitles[it] } ?: hash?.let { subtitleMap[it] }
        _currentSubtitleUrl.value = url
        if (url == null) {
            _subtitleLines.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val text = repository.getSubtitleText(url)
                _subtitleLines.value = parseSubtitle(text)
            } catch (_: Exception) {
                _subtitleLines.value = emptyList()
            }
        }
    }

    /** 手动匹配字幕：用户在字幕选择器中选中某个字幕文件 */
    fun selectSubtitle(subtitle: TrackNode) {
        val url = subtitle.mediaStreamUrl ?: return
        val track = playlistTracks.getOrNull(player.currentMediaItemIndex) ?: return
        val hash = track.hash ?: return
        manualSubtitles[hash] = url
        loadCurrentSubtitle()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    /** 相对跳转，正数前进 / 负数后退（毫秒） */
    fun seekBy(deltaMs: Long) {
        val max = player.duration.takeIf { it > 0L } ?: 0L
        player.seekTo((player.currentPosition + deltaMs).coerceIn(0L, max))
    }

    fun skipToNext() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
    }

    /** 跳转到队列中指定音轨 */
    fun jumpTo(index: Int) {
        player.seekTo(index, 0L)
        player.play()
    }

    /** 从队列移除指定音轨 */
    fun removeFromQueue(index: Int) {
        player.removeMediaItem(index)
        if (index < playlistTracks.size) {
            playlistTracks = playlistTracks.toMutableList().apply { removeAt(index) }
        }
        refreshQueue()
    }

    fun toggleShuffle() {
        val next = !_shuffleEnabled.value
        player.shuffleModeEnabled = next
        _shuffleEnabled.value = next
    }

    /** 关闭 -> 列表循环 -> 单曲循环 -> 关闭 */
    fun cycleRepeatMode() {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = next
        _repeatMode.value = next
    }

    /** 1.0 -> 1.25 -> 1.5 -> 2.0 -> 0.75 -> 1.0 */
    fun cycleSpeed() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
        val next = speeds[(speeds.indexOf(_speed.value) + 1) % speeds.size]
        player.playbackParameters = player.playbackParameters.withSpeed(next)
        _speed.value = next
    }

    /** 定时停止播放；传 null 取消 */
    fun setSleepTimer(minutes: Long?) {
        sleepJob?.cancel()
        if (minutes == null) {
            _sleepTimerActive.value = false
            return
        }
        _sleepTimerActive.value = true
        sleepJob = viewModelScope.launch {
            delay(minutes * 60_000)
            player.pause()
            _sleepTimerActive.value = false
        }
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _nowPlaying.value = null
        _subtitleLines.value = emptyList()
        _subtitleCandidates.value = emptyList()
        _currentSubtitleUrl.value = null
        manualSubtitles.clear()
        playlistWork = null
        playlistTracks = emptyList()
        _queue.value = QueueState()
    }

    override fun onCleared() {
        player.removeListener(listener)
        // 注意：不在此处 release，因为 Service 持有同一实例
    }
}
