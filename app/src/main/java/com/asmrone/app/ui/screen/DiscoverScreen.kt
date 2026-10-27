package com.asmrone.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asmrone.app.data.model.Work
import com.asmrone.app.ui.component.AppTopBar
import com.asmrone.app.ui.component.WorkCard

private val orderOptions = listOf(
    "create_date" to "最新",
    "dl_count" to "下载最多",
    "rate_average_2dp" to "评分最高",
)

/**
 * 发现页：浏览（排序 + 双列网格 + 下拉刷新）+ 顶部搜索框（搜索历史下拉 + 结果列表）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onWorkClick: (Work) -> Unit,
    pendingSearch: String? = null,
    onPendingSearchConsumed: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by homeViewModel.works.collectAsStateWithLifecycle()
    val currentOrder by homeViewModel.order.collectAsStateWithLifecycle()
    val results by searchViewModel.results.collectAsStateWithLifecycle()
    val searchHistory by settingsViewModel.searchHistory.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var searchMode by rememberSaveable { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // 搜索结果页按返回键 → 先退出搜索模式回到浏览，而不是退出应用
    BackHandler(enabled = searchMode) {
        searchMode = false
        query = ""
    }

    // 详情页标签/RJ/社团/CV 触发的搜索
    LaunchedEffect(pendingSearch) {
        if (pendingSearch != null) {
            query = pendingSearch
            searchMode = true
            searchViewModel.search(pendingSearch)
            onPendingSearchConsumed()
        }
    }

    fun executeSearch(kw: String) {
        if (kw.isBlank()) return
        query = kw
        searchMode = true
        focusManager.clearFocus()
        searchViewModel.search(kw)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar("发现")
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isBlank() && searchMode) searchMode = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .onFocusChanged { searchFocused = it.isFocused },
            placeholder = { Text("搜索作品名 / 社团 / 声优 / RJ号") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchMode || query.isNotEmpty()) {
                    IconButton(onClick = { searchMode = false; query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "清除搜索")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { executeSearch(query) }),
        )

        // 搜索历史下拉（聚焦且未输入时显示）
        if (searchFocused && query.isBlank() && searchHistory.isNotEmpty()) {
            SearchHistoryPanel(
                history = searchHistory,
                onPick = { executeSearch(it) },
                onClear = { settingsViewModel.clearSearchHistory() },
            )
        }

        if (!searchMode) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(orderOptions.size) { index ->
                    val (order, label) = orderOptions[index]
                    FilterChip(
                        selected = order == currentOrder,
                        onClick = { homeViewModel.setOrder(order) },
                        label = { Text(label) },
                    )
                }
            }
        }

        LaunchedEffect(state) {
            if (state !is UiState.Loading) isRefreshing = false
        }
        when {
            searchMode -> when (val s = results) {
                is UiState.Loading -> FullScreenLoading()
                is UiState.Error -> FullScreenError(s.message)
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        EmptyState("暂无结果")
                    } else {
                        WorksGrid(s.data, onWorkClick)
                    }
                }
            }
            else -> PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    homeViewModel.loadWorks()
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state) {
                    is UiState.Loading -> FullScreenLoading()
                    is UiState.Error -> FullScreenError(s.message)
                    is UiState.Success -> {
                        if (s.data.isEmpty()) EmptyState("暂无作品") else WorksGrid(s.data, onWorkClick)
                    }
                }
            }
        }
    }
}

/** 搜索历史下拉面板 */
@Composable
private fun SearchHistoryPanel(
    history: List<String>,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "搜索历史",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClear) {
                    Text(
                        "清空",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            history.take(10).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(item) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun WorksGrid(works: List<Work>, onWorkClick: (Work) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(works, key = { it.id ?: 0 }) { work ->
            WorkCard(
                work = work,
                onClick = { onWorkClick(work) },
            )
        }
    }
}

@Composable
private fun FullScreenLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FullScreenError(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
