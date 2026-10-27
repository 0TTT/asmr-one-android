package com.asmrone.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asmrone.app.data.model.Work
import com.asmrone.app.ui.component.MiniPlayerBar

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

/** 迷你播放器（约 78dp）+ 底部导航栏（约 80dp）的合计高度，用于内容区底部留白 */
private val BottomOverlayHeight = 158.dp

/**
 * 主界面：底部三 Tab（发现 / 收藏 / 设置）+ 悬浮迷你播放器 + 全屏播放页浮层。
 * 迷你播放器背景透明，悬浮在内容之上，列表滚动时内容可从其下透出。
 */
@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableStateOf(0) }
    var detailWork by remember { mutableStateOf<Work?>(null) }
    var showPlayer by remember { mutableStateOf(false) }
    // 详情页标签触发的搜索：切到发现页并自动执行
    var pendingSearch by remember { mutableStateOf<String?>(null) }

    // 返回键逐级返回：全屏播放页 → 详情页 → 列表；收藏/设置 Tab → 发现 Tab
    BackHandler(enabled = showPlayer) { showPlayer = false }
    BackHandler(enabled = !showPlayer && detailWork != null) { detailWork = null }
    BackHandler(enabled = !showPlayer && detailWork == null && selectedTab != 0) { selectedTab = 0 }

    Box(modifier = Modifier.fillMaxSize()) {
        // 内容区：底部预留迷你播放器 + 导航栏的空间
        Box(modifier = Modifier.fillMaxSize().padding(bottom = BottomOverlayHeight)) {
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

        // 底部叠加层：透明迷你播放器 + 导航栏
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            MiniPlayerBar(playerViewModel = playerViewModel, onExpand = { showPlayer = true })
            NavigationBar(tonalElevation = 3.dp) {
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
            )
        }
    }
}
