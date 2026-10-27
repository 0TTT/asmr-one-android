package com.asmrone.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asmrone.app.data.local.DownloadEntity
import com.asmrone.app.data.local.FavoriteEntity
import com.asmrone.app.data.local.PlayHistoryEntity
import com.asmrone.app.data.model.Work
import com.asmrone.app.ui.component.AppTopBar
import com.asmrone.app.ui.component.WorkRow
import kotlinx.coroutines.launch
import java.util.Locale

/** FavoriteEntity -> Work（用于跳转详情） */
private fun FavoriteEntity.toWork(): Work = Work(
    id = workId,
    title = title,
    sourceId = rjCode,
)

/** PlayHistoryEntity -> Work（用于跳转详情） */
private fun PlayHistoryEntity.toWork(): Work = Work(
    id = workId,
    title = title,
    sourceId = rjCode,
)

/** 总时长格式化，分钟可超过 59（如 102:36） */
private fun formatTotalTime(seconds: Int): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

private val libraryTabs = listOf("播放列表", "下载", "历史", "喜欢作品")

/**
 * 收藏页：二级 Tab（喜欢的作品 / 下载 / 播放列表 / 历史）。
 * 支持左右滑动切换，Tab 指示器与 Pager 双向联动。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onWorkClick: (Work) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { libraryTabs.size })
    val scope = rememberCoroutineScope()
    val selectedTab = pagerState.currentPage

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar("收藏")
        TabRow(selectedTabIndex = selectedTab) {
            libraryTabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            label,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> PlaylistTab(playerViewModel = playerViewModel)
                1 -> DownloadsTab(viewModel = viewModel)
                2 -> HistoryTab(viewModel = viewModel, onWorkClick = onWorkClick)
                3 -> FavoritesTab(viewModel = viewModel, onWorkClick = onWorkClick)
            }
        }
    }
}

// ---------- 喜欢的作品 ----------

private val sortLabels = listOf("最近保存", "最早保存", "评分最高")

@Composable
private fun FavoritesTab(viewModel: LibraryViewModel, onWorkClick: (Work) -> Unit) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    var sortMode by rememberSaveable { mutableStateOf(0) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val sorted = remember(favorites, sortMode) {
        when (sortMode) {
            1 -> favorites.sortedBy { it.addedAt }
            2 -> favorites.sortedByDescending { it.rating }
            else -> favorites.sortedByDescending { it.addedAt }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${favorites.size} 部作品",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            Box {
                TextButton(onClick = { sortMenuOpen = true }) {
                    Text(sortLabels[sortMode], color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    sortLabels.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                sortMode = index
                                sortMenuOpen = false
                            },
                        )
                    }
                }
            }
        }

        if (sorted.isEmpty()) {
            EmptyState("暂无收藏")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 84.dp)) {
                items(sorted, key = { it.workId }) { fav ->
                    FavoriteRow(
                        favorite = fav,
                        onClick = { onWorkClick(fav.toWork()) },
                        onUnfavorite = { viewModel.removeFavorite(fav.workId) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(
    favorite: FavoriteEntity,
    onClick: () -> Unit,
    onUnfavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (favorite.coverUrl != null) {
                AsyncImage(
                    model = favorite.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = favorite.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = favorite.circleName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = formatTotalTime(favorite.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = String.format(Locale.US, "%.1f", favorite.rating),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onUnfavorite) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "取消收藏",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ---------- 下载 ----------

@Composable
private fun DownloadsTab(viewModel: LibraryViewModel) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    if (downloads.isEmpty()) {
        EmptyState("暂无下载")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 84.dp)) {
            items(downloads, key = { it.trackUrl }) { d ->
                DownloadRow(
                    entity = d,
                    onDelete = { viewModel.removeDownload(d.trackUrl) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DownloadRow(entity: DownloadEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entity.trackTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${entity.workTitle}  ·  ${entity.fileSize / 1024 / 1024} MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
        }
    }
}

// ---------- 播放列表（当前播放队列） ----------

@Composable
private fun PlaylistTab(playerViewModel: PlayerViewModel) {
    val queue by playerViewModel.queue.collectAsStateWithLifecycle()
    if (queue.entries.isEmpty()) {
        EmptyState("暂无播放列表")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 84.dp)) {
            items(queue.entries, key = { it.index }) { entry ->
                val isCurrent = entry.index == queue.currentIndex
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clickable { playerViewModel.jumpTo(entry.index) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "${entry.index + 1}",
                            color = if (isCurrent) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(
                            text = entry.title.ifEmpty { "未命名音轨" },
                            color = if (isCurrent) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// ---------- 历史 ----------

@Composable
private fun HistoryTab(viewModel: LibraryViewModel, onWorkClick: (Work) -> Unit) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            if (history.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("清空历史", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (history.isEmpty()) {
            EmptyState("暂无历史")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 84.dp)) {
                items(history, key = { it.id }) { h ->
                    WorkRow(
                        title = h.title,
                        subtitle = "${h.circleName}  ${h.rjCode}" + (h.trackTitle?.let { "  ·  $it" } ?: ""),
                        coverUrl = h.coverUrl,
                        onClick = { onWorkClick(h.toWork()) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
