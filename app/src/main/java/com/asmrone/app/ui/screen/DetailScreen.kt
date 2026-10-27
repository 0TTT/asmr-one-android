package com.asmrone.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asmrone.app.data.model.Tag
import com.asmrone.app.data.model.TrackNode
import com.asmrone.app.data.model.Work
import com.asmrone.app.di.DownloadManagerEntryPoint
import com.asmrone.app.ui.component.formatDuration
import dagger.hilt.android.EntryPointAccessors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    work: Work,
    onBack: () -> Unit,
    onTagSearch: (String) -> Unit = {},
    playerViewModel: PlayerViewModel = hiltViewModel(),
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val treeState by viewModel.tree.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val downloadManager = EntryPointAccessors
        .fromApplication(context.applicationContext, DownloadManagerEntryPoint::class.java)
        .downloadManager()

    androidx.compose.runtime.LaunchedEffect(work.id) {
        viewModel.loadTracks(work)
    }

    // 收藏状态
    val isFavorite by viewModel.observeFavorite(work.id ?: 0)
        .collectAsStateWithLifecycle(initialValue = false)

    // 下载模式：行尾出现复选框，选择后确认下载
    var isDownloadMode by rememberSaveable { mutableStateOf(false) }
    var selectedHashes by rememberSaveable { mutableStateOf(setOf<String>()) }

    // 下载模式下按返回键 → 先退出下载模式（MainScreen 的返回逻辑在其后）
    BackHandler(enabled = isDownloadMode) {
        isDownloadMode = false
        selectedHashes = emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(work.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(work) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            item { WorkHeader(work, onTagSearch) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("文件列表", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    val rows = (treeState as? UiState.Success)?.let { flattenTree(it.data) } ?: emptyList()
                    if (rows.isNotEmpty()) {
                        if (!isDownloadMode) {
                            FilledTonalButton(onClick = { isDownloadMode = true }) {
                                Icon(Icons.Filled.Download, contentDescription = null)
                                Spacer(Modifier.size(4.dp))
                                Text("下载")
                            }
                        } else {
                            Button(
                                onClick = {
                                    val nodes = selectedHashes.mapNotNull { h ->
                                        rows.firstOrNull { r -> r.node.hash == h }?.node
                                    }
                                    nodes.forEach { downloadManager.download(work, it) }
                                    android.widget.Toast.makeText(
                                        context,
                                        "已加入下载队列 ${nodes.size} 个文件",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                    isDownloadMode = false
                                    selectedHashes = emptySet()
                                },
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null)
                                Spacer(Modifier.size(4.dp))
                                Text("确认下载(${selectedHashes.size})")
                            }
                        }
                    }
                }
            }

            when (val ts = treeState) {
                is UiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(ts.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val rows = flattenTree(ts.data)
                    items(
                        rows,
                        key = { r -> r.node.hash ?: "${r.depth}_${r.node.type}_${r.node.title}" },
                    ) { row ->
                        TreeFileRow(
                            row = row,
                            isDownloadMode = isDownloadMode,
                            isChecked = row.node.hash != null && row.node.hash in selectedHashes,
                            onPlay = {
                                // 点击某个音频 → 自动把同一文件夹内的所有音频按顺序拉入播放队列
                                val siblings = rows
                                    .filter { it.parent == row.parent && it.node.isAudio }
                                    .map { it.node }
                                val idx = siblings.indexOfFirst { it.hash == row.node.hash }
                                playerViewModel.playAll(work, siblings, idx.coerceAtLeast(0))
                            },
                            onToggle = {
                                val node = row.node
                                if (node.isFolder) {
                                    val hashes = collectFileHashes(node)
                                    val allChecked = hashes.isNotEmpty() && hashes.all { it in selectedHashes }
                                    selectedHashes =
                                        if (allChecked) selectedHashes - hashes else selectedHashes + hashes
                                } else {
                                    node.hash?.let { h ->
                                        selectedHashes =
                                            if (h in selectedHashes) selectedHashes - h else selectedHashes + h
                                    }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** 文件类型对应图标 */
private fun fileIcon(node: TrackNode): ImageVector = when {
    node.isFolder -> Icons.Filled.Folder
    node.isAudio -> Icons.Filled.MusicNote
    node.isSubtitle -> Icons.Filled.Subtitles
    node.title?.endsWith(".pdf", true) == true -> Icons.Filled.PictureAsPdf
    node.type == "image" -> Icons.Filled.Image
    node.type == "text" -> Icons.Filled.Description
    else -> Icons.Filled.InsertDriveFile
}

/**
 * 文件树单行：缩进表示层级；点击音频播放（同文件夹音频成队）；
 * 下载模式下显示复选框（文件夹 = 递归全选其下文件）。
 */
@Composable
private fun TreeFileRow(
    row: TreeRow,
    isDownloadMode: Boolean,
    isChecked: Boolean,
    onPlay: () -> Unit,
    onToggle: () -> Unit,
) {
    val node = row.node
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable {
                when {
                    isDownloadMode -> onToggle()
                    node.isAudio -> onPlay()
                }
            }
            .padding(start = 16.dp + 20.dp * row.depth, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = fileIcon(node),
            contentDescription = null,
            tint = if (node.isFolder) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.title ?: "未命名",
                style = MaterialTheme.typography.bodyMedium,
                color = if (node.isFolder) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            node.size?.let { size ->
                Text(
                    formatSize(size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when {
            isDownloadMode && node.isFile -> Checkbox(checked = isChecked, onCheckedChange = { onToggle() })
            !isDownloadMode && node.isAudio -> Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "播放",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / 1024f / 1024f
    return if (mb >= 1f) String.format(Locale.US, "%.1f MB", mb) else "${bytes / 1024} KB"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkHeader(work: Work, onTagSearch: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (work.coverUrl != null) {
                    AsyncImage(
                        model = work.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ClickableCopyText(
                    text = work.rjCode,
                    searchText = work.rjCode,
                    onSearch = onTagSearch,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                ClickableCopyText(
                    text = work.circleName,
                    searchText = "\$circle:${work.circleName}\$",
                    onSearch = onTagSearch,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (work.vaNames.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "CV:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        work.vaNames.forEach { name ->
                            ClickableCopyText(
                                text = name,
                                searchText = "\$va:$name\$",
                                onSearch = onTagSearch,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Text(
                        String.format(Locale.US, "%.2f", work.rating),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if ((work.duration ?: 0) > 0) {
                    Text("时长 ${formatDuration(work.duration)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (work.release != null) {
                    Text("发售 ${work.release}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (work.tags.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                work.tags.forEach { tag ->
                    TagChip(tag = tag, onTagSearch = onTagSearch)
                }
            }
        }
    }
}

/**
 * 可交互文本：点击搜索对应内容，长按弹出「复制 / 搜索」菜单。
 * 用于 RJ 号、社团名、CV 名等。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClickableCopyText(
    text: String,
    searchText: String,
    onSearch: (String) -> Unit,
    style: TextStyle,
    color: Color,
) {
    if (text.isBlank()) return
    var menuOpen by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    Box {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.combinedClickable(
                onClick = { onSearch(searchText) },
                onLongClick = { menuOpen = true },
            ),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("复制") },
                onClick = {
                    clipboard.setText(AnnotatedString(text))
                    menuOpen = false
                },
            )
            DropdownMenuItem(
                text = { Text("搜索") },
                onClick = {
                    onSearch(searchText)
                    menuOpen = false
                },
            )
        }
    }
}

/**
 * 可交互的标签词条：
 * - 点击：直接搜索该标签下的所有作品
 * - 长按：菜单（复制标签 / 搜索标签 / 排除标签）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagChip(tag: Tag, onTagSearch: (String) -> Unit) {
    val name = tag.displayName
    if (name.isBlank()) return
    var menuOpen by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Box {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .combinedClickable(
                    onClick = { onTagSearch("\$tag:$name\$") },
                    onLongClick = { menuOpen = true },
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("复制标签") },
                onClick = {
                    clipboard.setText(AnnotatedString(name))
                    menuOpen = false
                },
            )
            DropdownMenuItem(
                text = { Text("搜索标签") },
                onClick = {
                    onTagSearch("\$tag:$name\$")
                    menuOpen = false
                },
            )
            DropdownMenuItem(
                text = { Text("排除标签") },
                onClick = {
                    onTagSearch("\$-tag:$name\$")
                    menuOpen = false
                },
            )
        }
    }
}
