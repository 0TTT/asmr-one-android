package com.asmrone.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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
 * 发现页：浏览（排序筛选 + 双列网格）+ 顶部搜索框（搜索时切换为结果列表）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onWorkClick: (Work) -> Unit,
    pendingSearch: String? = null,
    onPendingSearchConsumed: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    filterViewModel: FilterViewModel = hiltViewModel(),
) {
    val state by homeViewModel.works.collectAsStateWithLifecycle()
    val currentOrder by homeViewModel.order.collectAsStateWithLifecycle()
    val results by searchViewModel.results.collectAsStateWithLifecycle()
    val onlyFavorites by filterViewModel.onlyFavorites.collectAsStateWithLifecycle()
    val favoriteWorkIds by filterViewModel.favoriteWorkIds.collectAsStateWithLifecycle()
    val subtitleOnly by filterViewModel.subtitleOnly.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    var searchMode by rememberSaveable { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // 搜索结果页按返回键 → 先退出搜索模式回到浏览，而不是退出应用
    BackHandler(enabled = searchMode) {
        searchMode = false
        query = ""
    }

    // 详情页标签触发的搜索（点击标签 / 搜索标签 / 排除标签）
    androidx.compose.runtime.LaunchedEffect(pendingSearch) {
        if (pendingSearch != null) {
            query = pendingSearch
            searchMode = true
            searchViewModel.search(pendingSearch, filterViewModel.subtitleOnly.value)
            onPendingSearchConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar("发现") {
            IconButton(onClick = { showFilter = true }) {
                Icon(Icons.Filled.Tune, contentDescription = "筛选")
            }
        }
        OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isBlank() && searchMode) searchMode = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("搜索作品名 / 社团 / 声优 / RJ号") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchMode) {
                        IconButton(onClick = { searchMode = false; query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "清除搜索")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (query.isNotBlank()) {
                            searchMode = true
                            searchViewModel.search(filterViewModel.buildQuery(query), subtitleOnly)
                        }
                    }
                ),
            )

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

            androidx.compose.runtime.LaunchedEffect(state) {
                if (state !is UiState.Loading) isRefreshing = false
            }
            when {
                searchMode -> when (val s = results) {
                    is UiState.Loading -> FullScreenLoading()
                    is UiState.Error -> FullScreenError(s.message)
                    is UiState.Success -> {
                        val shown = if (onlyFavorites) s.data.filter { it.id in favoriteWorkIds } else s.data
                        if (shown.isEmpty()) {
                            EmptyState(if (s.data.isEmpty()) "暂无结果" else "收藏中暂无匹配")
                        } else {
                            WorksGrid(shown, onWorkClick)
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
                            val shown = if (onlyFavorites) s.data.filter { it.id in favoriteWorkIds } else s.data
                            if (shown.isEmpty()) EmptyState("收藏中暂无作品") else WorksGrid(shown, onWorkClick)
                        }
                    }
                }
            }
        }

    if (showFilter) {
        FilterSheet(
            onDismiss = { showFilter = false },
            onApply = {
                showFilter = false
                val q = filterViewModel.buildQuery(query)
                if (q.isNotBlank()) {
                    searchMode = true
                    searchViewModel.search(q, filterViewModel.subtitleOnly.value)
                }
            },
        )
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
