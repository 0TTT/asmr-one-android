package com.asmrone.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.asmrone.app.data.model.currentIndexFor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import java.util.Locale

// 播放页固定深色配色（参考 asmr.one 类播放器视觉）
private val PlayerBg = Color(0xFF171210)
private val PlayerSurface = Color(0xFF272019)
private val PlayerSurfaceHigh = Color(0xFF3A2D24)
private val PlayerAccent = Color(0xFFF2A48E)
private val PlayerOnAccent = Color(0xFF2A1A13)
private val PlayerTextPrimary = Color(0xFFF0EAE3)
private val PlayerTextSecondary = Color(0xFF9A8F86)

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

private fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) String.format(Locale.US, "%.1f×", speed)
    else String.format(Locale.US, "%.2f×", speed)

/**
 * 全屏播放页（点击迷你播放器展开）。
 * 布局自上而下：封面 -> 字幕区 -> 进度条 -> 主控制 -> 次要控制 -> 播放队列，整页可滚动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit,
    onCoverClick: () -> Unit = {},
) {
    val nowPlaying by playerViewModel.nowPlaying.collectAsStateWithLifecycle()
    val (work, track) = nowPlaying ?: return

    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by playerViewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by playerViewModel.durationMs.collectAsStateWithLifecycle()
    val queue by playerViewModel.queue.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleEnabled by playerViewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val speed by playerViewModel.speed.collectAsStateWithLifecycle()
    val sleepActive by playerViewModel.sleepTimerActive.collectAsStateWithLifecycle()
    val subtitleLines by playerViewModel.subtitleLines.collectAsStateWithLifecycle()
    val subtitleCandidates by playerViewModel.subtitleCandidates.collectAsStateWithLifecycle()
    val currentSubtitleUrl by playerViewModel.currentSubtitleUrl.collectAsStateWithLifecycle()

    var editQueue by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableStateOf(0f) }
    var showSubtitlePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // 顶栏：关闭 + 作品信息
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "关闭", tint = PlayerTextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = work.title ?: "",
                    color = PlayerTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = work.circleName,
                    color = PlayerTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 封面（当前音轨名叠在左下角）；点击封面回到作品详情页
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(PlayerSurface)
                .clickable(onClick = onCoverClick),
        ) {
            if (work.coverUrl != null) {
                AsyncImage(
                    model = work.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 12.dp),
            ) {
                Text(
                    text = track.title ?: "",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 字幕展示区：显示当前进度对应的字幕行
        val currentSubtitleLine = remember(subtitleLines, positionMs) {
            val idx = subtitleLines.currentIndexFor(positionMs)
            if (idx >= 0) subtitleLines[idx].text else null
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = currentSubtitleLine ?: "未检测到字幕",
                color = if (currentSubtitleLine != null) PlayerTextPrimary else PlayerTextSecondary,
                fontSize = 15.sp,
                fontWeight = if (currentSubtitleLine != null) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center,
            )
        }

        // 进度条 + 时间（右侧为剩余倒计时）
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            val maxValue = durationMs.coerceAtLeast(1L).toFloat()
            Slider(
                value = (if (dragging) dragPos else positionMs.toFloat()).coerceIn(0f, maxValue),
                onValueChange = {
                    dragging = true
                    dragPos = it
                },
                onValueChangeFinished = {
                    playerViewModel.seekTo(dragPos.toLong())
                    dragging = false
                },
                valueRange = 0f..maxValue,
                colors = SliderDefaults.colors(
                    thumbColor = PlayerAccent,
                    activeTrackColor = PlayerAccent,
                    inactiveTrackColor = PlayerSurfaceHigh,
                ),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(formatTime(positionMs), color = PlayerTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                val remaining = (durationMs - positionMs).coerceAtLeast(0L)
                Text("-" + formatTime(remaining), color = PlayerTextSecondary, fontSize = 12.sp)
            }
        }

        // 主控制：上一曲 / 快退5s / 播放暂停 / 快进5s / 下一曲
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { playerViewModel.skipToPrevious() }) {
                Icon(Icons.Filled.SkipPrevious, "上一曲", tint = PlayerTextPrimary, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { playerViewModel.seekBy(-5_000) }) {
                Icon(Icons.Filled.Replay5, "后退5秒", tint = PlayerTextPrimary, modifier = Modifier.size(28.dp))
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(PlayerAccent)
                    .clickable { playerViewModel.togglePlayPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "播放/暂停",
                    tint = PlayerOnAccent,
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = { playerViewModel.seekBy(5_000) }) {
                Icon(Icons.Filled.Forward5, "前进5秒", tint = PlayerTextPrimary, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = { playerViewModel.skipToNext() }) {
                Icon(Icons.Filled.SkipNext, "下一曲", tint = PlayerTextPrimary, modifier = Modifier.size(32.dp))
            }
        }

        // 次要控制胶囊：随机 / 循环 / 倍速 / 定时
        Surface(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = PlayerSurface,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { playerViewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "随机播放",
                        tint = if (shuffleEnabled) PlayerAccent else PlayerTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = { playerViewModel.cycleRepeatMode() }) {
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "循环模式",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) PlayerAccent else PlayerTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = speedLabel(speed),
                    color = if (speed != 1.0f) PlayerAccent else PlayerTextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { playerViewModel.cycleSpeed() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                SleepTimerButton(playerViewModel = playerViewModel, active = sleepActive)
                IconButton(onClick = { showSubtitlePicker = true }) {
                    Icon(
                        Icons.Filled.ClosedCaption,
                        contentDescription = "手动匹配字幕",
                        tint = if (currentSubtitleUrl != null) PlayerAccent else PlayerTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // 播放队列
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("播放队列", color = PlayerTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            Text("${queue.entries.size}", color = PlayerTextSecondary, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { editQueue = !editQueue }) {
                Text(
                    text = if (editQueue) "完成" else "编辑队列",
                    color = PlayerTextSecondary,
                    fontSize = 13.sp,
                )
            }
        }

        queue.entries.forEach { entry ->
            val isCurrent = entry.index == queue.currentIndex
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .clickable { playerViewModel.jumpTo(entry.index) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) PlayerSurfaceHigh else PlayerSurface
                ),
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${entry.index + 1}",
                        color = if (isCurrent) PlayerAccent else PlayerTextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(
                        text = entry.title.ifEmpty { "未命名音轨" },
                        color = if (isCurrent) PlayerTextPrimary else PlayerTextSecondary,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (editQueue) {
                        IconButton(onClick = { playerViewModel.removeFromQueue(entry.index) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "移除",
                                tint = PlayerTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }

    // 手动匹配字幕：列出该作品全部字幕文件
    if (showSubtitlePicker) {
        ModalBottomSheet(onDismissRequest = { showSubtitlePicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    text = track.title ?: "选择字幕",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))

                if (subtitleCandidates.isEmpty()) {
                    Text(
                        text = "该作品没有可用的字幕文件",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    subtitleCandidates.forEach { sub ->
                        val selected = sub.mediaStreamUrl != null && sub.mediaStreamUrl == currentSubtitleUrl
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { playerViewModel.selectSubtitle(sub) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = sub.title ?: "未命名字幕",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            RadioButton(
                                selected = selected,
                                onClick = { playerViewModel.selectSubtitle(sub) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerButton(playerViewModel: PlayerViewModel, active: Boolean) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                Icons.Filled.Timer,
                contentDescription = "定时停止",
                tint = if (active) PlayerAccent else PlayerTextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("关闭定时") },
                onClick = {
                    playerViewModel.setSleepTimer(null)
                    menuOpen = false
                },
            )
            listOf(15L, 30L, 60L).forEach { minutes ->
                DropdownMenuItem(
                    text = { Text("$minutes 分钟后停止") },
                    onClick = {
                        playerViewModel.setSleepTimer(minutes)
                        menuOpen = false
                    },
                )
            }
        }
    }
}
