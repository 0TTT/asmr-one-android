package com.asmrone.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asmrone.app.data.model.FilterItem
import java.util.Locale

/**
 * 筛选面板（ModalBottomSheet）：
 * 顶部分类 Tab（标签/社团/声优）→ 列表内搜索 → 全部/收藏 → 语言·字幕 → 数量/名称排序 + 排除模式 → 条目列表 → 重置/应用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    filterViewModel: FilterViewModel = hiltViewModel(),
) {
    val selectedType by filterViewModel.selectedType.collectAsStateWithLifecycle()
    val listState by filterViewModel.listState.collectAsStateWithLifecycle()
    val included by filterViewModel.included.collectAsStateWithLifecycle()
    val excluded by filterViewModel.excluded.collectAsStateWithLifecycle()
    val excludeMode by filterViewModel.excludeMode.collectAsStateWithLifecycle()
    val subtitleOnly by filterViewModel.subtitleOnly.collectAsStateWithLifecycle()
    val onlyFavorites by filterViewModel.onlyFavorites.collectAsStateWithLifecycle()
    val sortByCount by filterViewModel.sortByCount.collectAsStateWithLifecycle()

    var listQuery by remember { mutableStateOf("") }
    var langExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "筛选",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 分类 Tab：标签 / 社团 / 声优
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                FilterType.entries.forEach { type ->
                    val selected = type == selectedType
                    val tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier
                            .clickable { filterViewModel.selectType(type) }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(filterIcon(type), contentDescription = type.label, tint = tint, modifier = Modifier.size(22.dp))
                        Text(type.label, fontSize = 12.sp, color = tint)
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(width = 40.dp, height = 2.dp)
                                .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                        )
                    }
                }
            }

            // 列表内搜索
            OutlinedTextField(
                value = listQuery,
                onValueChange = { listQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("在列表中搜索名称", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
            )

            // 全部 / 收藏 分段选择
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
            ) {
                SegmentText(
                    text = "全部",
                    selected = !onlyFavorites,
                    modifier = Modifier.weight(1f),
                    onClick = { filterViewModel.onlyFavorites.value = false },
                )
                SegmentText(
                    text = "收藏",
                    selected = onlyFavorites,
                    modifier = Modifier.weight(1f),
                    onClick = { filterViewModel.onlyFavorites.value = true },
                )
            }

            // 作品语言·字幕（可展开）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { langExpanded = !langExpanded }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text("作品语言 · 字幕", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Icon(
                    if (langExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (langExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { filterViewModel.subtitleOnly.value = !subtitleOnly }
                        .padding(start = 40.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = subtitleOnly,
                        onCheckedChange = { filterViewModel.subtitleOnly.value = it },
                    )
                    Text("仅显示有字幕的作品", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            HorizontalDivider()

            // 排序切换 + 排除项目
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SortTab("作品数量排序", sortByCount, Modifier.weight(1f)) { filterViewModel.sortByCount.value = true }
                SortTab("按名称", !sortByCount, Modifier.weight(1f)) { filterViewModel.sortByCount.value = false }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (excludeMode) MaterialTheme.colorScheme.errorContainer else androidx.compose.ui.graphics.Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.clickable { filterViewModel.excludeMode.value = !excludeMode },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Block,
                            contentDescription = null,
                            tint = if (excludeMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(4.dp))
                        Text("排除项目", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 条目列表
            Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                when (val s = listState) {
                    is UiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    is UiState.Success -> {
                        val inc = included[selectedType] ?: emptySet()
                        val exc = excluded[selectedType] ?: emptySet()
                        val filtered = s.data
                            .asSequence()
                            .filter { listQuery.isBlank() || it.displayName.contains(listQuery, ignoreCase = true) }
                            .sortedWith(
                                if (sortByCount) compareByDescending<FilterItem> { it.displayCount ?: 0 }
                                else compareBy { it.displayName }
                            )
                            .toList()
                        if (filtered.isEmpty()) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn {
                                items(filtered, key = { "${it.id ?: it.displayName}" }) { item ->
                                    FilterItemRow(
                                        item = item,
                                        checked = item.displayName in inc,
                                        excluded = item.displayName in exc,
                                        favorited = filterViewModel.isFavoriteFilter(selectedType, item),
                                        onToggle = { filterViewModel.toggleItem(selectedType, item.displayName) },
                                        onFavorite = { filterViewModel.toggleFavoriteFilter(selectedType, item) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 底部操作
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { filterViewModel.reset() },
                    modifier = Modifier.weight(0.35f),
                ) {
                    Text("重置")
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(0.65f),
                ) {
                    Text("应用并搜索")
                    Spacer(Modifier.size(4.dp))
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun filterIcon(type: FilterType) = when (type) {
    FilterType.TAG -> Icons.Filled.Label
    FilterType.CIRCLE -> Icons.Filled.Domain
    FilterType.VA -> Icons.Filled.Mic
}

@Composable
private fun SegmentText(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceContainerHigh
                else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 14.sp,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SortTab(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            fontSize = 13.sp,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(width = 56.dp, height = 2.dp)
                .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}

@Composable
private fun FilterItemRow(
    item: FilterItem,
    checked: Boolean,
    excluded: Boolean,
    favorited: Boolean,
    onToggle: () -> Unit,
    onFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked || excluded,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = if (excluded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            ),
        )
        Text(
            text = item.displayName,
            fontSize = 14.sp,
            color = if (excluded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        item.displayCount?.let {
            Text(
                text = String.format(Locale.US, "%,d", it),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (favorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "收藏该条件",
                tint = if (favorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
