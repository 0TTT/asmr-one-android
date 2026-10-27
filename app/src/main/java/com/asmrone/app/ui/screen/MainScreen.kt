package com.asmrone.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asmrone.app.data.model.Work
import com.asmrone.app.ui.component.MiniPlayerBar
import kotlinx.serialization.json.Json

/** 底部导航栏高度（内容区预留），迷你播放器悬浮在其上方、不占位 */
private val NavHeight = 80.dp

/** detailWork 的进程死亡恢复：Work 序列化为 JSON 字符串存入 Bundle */
private val detailJson = Json { ignoreUnknownKeys = true }

private val detailWorkSaver = Saver<Work?, String>(
    save = { state ->
        state?.let { w -> runCatching { detailJson.encodeToString(Work.serializer(), w) }.getOrNull() } ?: ""
    },
    restore = { str ->
        str.takeIf { it.isNotEmpty() }?.let { s ->
            runCatching { detailJson.decodeFromString(Work.serializer(), s) }.getOrNull()
        }
    },
)

/** 底部导航项 */
private data class Tab(
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    Tab("发现", Icons.Filled.Explore),
    Tab("收藏", Icons.Filled.Favorite),
    Tab("设置", Icons.Filled.Settings),
)

/**
 * 主界面：底部三 Tab + 悬浮迷你播放器 + 全屏播放页浮层。
 * 内容区只预留导航栏高度；迷你播放器绝对悬浮、背景透明，列表内容可从其下透出。
 */
@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var pendingSearch by rememberSaveable { mutableStateOf<String?>(null) }
    var detailWork by rememberSaveable(stateSaver = detailWorkSaver) { mutableStateOf<Work?>(null) }

    // 返回键逐级返回：全屏播放页 → 详情页 → 列表；收藏/设置 Tab → 发现 Tab
    BackHandler(enabled = showPlayer) { showPlayer = false }
    BackHandler(enabled = !showPlayer && detailWork != null) { detailWork = null }
    BackHandler(enabled = !showPlayer && detailWork == null && selectedTab != 0) { selectedTab = 0 }

    Box(modifier = Modifier.fillMaxSize()) {
        // 内容区：只预留导航栏高度，迷你播放器悬浮在内容之上
        Box(modifier = Modifier.fillMaxSize().padding(bottom = NavHeight)) {
            val work = detailWork
            if (work != null) {
                DetailScreen(
                    work = work,
                    onBack = { detailWork = null },
                    onTagSearch = { query ->
                        pendingSearch = query
                        detailWork = null
                        selectedTab = 0
                    },
                    playerViewModel = playerViewModel,
                )
            } else {
                when (selectedTab) {
                    0 -> DiscoverScreen(
                        onWorkClick = { work -> detailWork = work },
                        pendingSearch = pendingSearch,
                        onPendingSearchConsumed = { pendingSearch = null },
                    )
                    1 -> LibraryScreen(onWorkClick = { work -> detailWork = work })
                    2 -> SettingsScreen()
                }
            }
        }

        // 迷你播放器：绝对悬浮在导航栏上方，背景透明，内容可从其下透出
        MiniPlayerBar(
            playerViewModel = playerViewModel,
            onExpand = { showPlayer = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = NavHeight),
        )

        // 底部导航栏
        NavigationBar(
            tonalElevation = 3.dp,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        detailWork = null
                    },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) },
                )
            }
        }

        // 全屏播放页浮层：从迷你播放器所在的底部滑入，关闭时收回
        AnimatedVisibility(
            visible = showPlayer,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            PlayerScreen(
                playerViewModel = playerViewModel,
                onClose = { showPlayer = false },
                onCoverClick = {
                    // 点击全屏播放页封面 → 回到该作品详情页
                    showPlayer = false
                    playerViewModel.nowPlaying.value?.first?.let { detailWork = it }
                },
            )
        }
    }
}
