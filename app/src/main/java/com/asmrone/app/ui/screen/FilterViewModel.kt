package com.asmrone.app.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asmrone.app.data.SettingsRepository
import com.asmrone.app.data.model.FilterItem
import com.asmrone.app.data.repository.AsmrOneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 筛选类别 */
enum class FilterType(val label: String) {
    TAG("标签"),
    CIRCLE("社团"),
    VA("声优"),
}

/**
 * 筛选面板状态。
 * 选中项按「名称」记录：asmr.one 搜索语法的 tag/va/circle 过滤值即名称。
 */
@HiltViewModel
class FilterViewModel @Inject constructor(
    private val repository: AsmrOneRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // ---- 列表加载（按类别懒加载 + 缓存） ----

    private val cache = mutableMapOf<FilterType, List<FilterItem>>()

    private val _selectedType = MutableStateFlow(FilterType.TAG)
    val selectedType: StateFlow<FilterType> = _selectedType.asStateFlow()

    private val _listState = MutableStateFlow<UiState<List<FilterItem>>>(UiState.Loading)
    val listState: StateFlow<UiState<List<FilterItem>>> = _listState.asStateFlow()

    fun selectType(type: FilterType) {
        _selectedType.value = type
        load(type)
    }

    fun load(type: FilterType) {
        cache[type]?.let {
            _listState.value = UiState.Success(it)
            return
        }
        viewModelScope.launch {
            _listState.value = UiState.Loading
            val items = when (type) {
                FilterType.TAG -> runCatching { repository.getTags() }.getOrNull()
                FilterType.CIRCLE -> runCatching { repository.getCircles() }.getOrNull()
                FilterType.VA -> runCatching { repository.getVas() }.getOrNull()
            }
            if (items != null) {
                cache[type] = items
                _listState.value = UiState.Success(items)
            } else {
                // /tags /circles /vas 需要登录认证（未登录返回 401）。
                // 降级方案：聚合热门作品的标签/社团/声优与本地引用计数，无需登录即可用。
                val agg = aggregateFallback()
                agg.forEach { (t, list) -> cache[t] = list }
                val fallback = agg[type]
                if (fallback != null) {
                    _listState.value = UiState.Success(fallback)
                } else {
                    _listState.value = UiState.Error("加载失败，请检查网络")
                }
            }
        }
    }

    /** 聚合热门作品（按下载量前几页）的标签/社团/声优引用计数，作为筛选列表的无登录兜底 */
    private suspend fun aggregateFallback(): Map<FilterType, List<FilterItem>> {
        val tagCount = linkedMapOf<String, Int>()
        val circleCount = linkedMapOf<String, Int>()
        val vaCount = linkedMapOf<String, Int>()
        for (page in 1..4) {
            runCatching { repository.getWorks(page = page, order = "dl_count", sort = "desc") }
                .getOrNull()?.works?.forEach { w ->
                    w.tags.forEach { t ->
                        val n = t.displayName
                        if (n.isNotBlank()) tagCount[n] = (tagCount[n] ?: 0) + 1
                    }
                    w.circleName.takeIf { it.isNotBlank() }?.let {
                        circleCount[it] = (circleCount[it] ?: 0) + 1
                    }
                    w.vas.forEach { v ->
                        v.name?.takeIf { n -> n.isNotBlank() }?.let {
                            vaCount[it] = (vaCount[it] ?: 0) + 1
                        }
                    }
                }
        }
        return mapOf(
            FilterType.TAG to tagCount.entries
                .sortedByDescending { it.value }
                .map { FilterItem(name = it.key, count = it.value) },
            FilterType.CIRCLE to circleCount.entries
                .sortedByDescending { it.value }
                .map { FilterItem(name = it.key, count = it.value) },
            FilterType.VA to vaCount.entries
                .sortedByDescending { it.value }
                .map { FilterItem(name = it.key, count = it.value) },
        )
    }

    // ---- 选择状态 ----

    /** 包含：type -> 名称集合 */
    private val _included = MutableStateFlow<Map<FilterType, Set<String>>>(emptyMap())
    val included: StateFlow<Map<FilterType, Set<String>>> = _included.asStateFlow()

    /** 排除：type -> 名称集合 */
    private val _excluded = MutableStateFlow<Map<FilterType, Set<String>>>(emptyMap())
    val excluded: StateFlow<Map<FilterType, Set<String>>> = _excluded.asStateFlow()

    /** 排除模式：开启后点击条目记为排除（-前缀） */
    val excludeMode = MutableStateFlow(false)

    /** 仅显示有字幕作品 */
    val subtitleOnly = MutableStateFlow(false)

    /** 只看收藏的作品（客户端过滤） */
    val onlyFavorites = MutableStateFlow(false)

    /** 列表排序：true=按作品数量，false=按名称 */
    val sortByCount = MutableStateFlow(true)

    fun toggleItem(type: FilterType, name: String) {
        if (name.isBlank()) return
        if (excludeMode.value) {
            _excluded.value = _excluded.value.toMutableMap().apply {
                val cur = get(type) ?: emptySet()
                put(type, if (name in cur) cur - name else cur + name)
            }
            // 排除与包含互斥
            _included.value = _included.value.toMutableMap().apply {
                get(type)?.let { put(type, it - name) }
            }
        } else {
            _included.value = _included.value.toMutableMap().apply {
                val cur = get(type) ?: emptySet()
                put(type, if (name in cur) cur - name else cur + name)
            }
            _excluded.value = _excluded.value.toMutableMap().apply {
                get(type)?.let { put(type, it - name) }
            }
        }
    }

    /** 收藏的筛选条件（DataStore 持久化） */
    val favoriteFilters: StateFlow<Set<String>> = settingsRepository.favoriteFilters

    fun toggleFavoriteFilter(type: FilterType, item: FilterItem) {
        settingsRepository.toggleFavoriteFilter("${type.name}:${item.id ?: item.displayName}")
    }

    fun isFavoriteFilter(type: FilterType, item: FilterItem): Boolean =
        "${type.name}:${item.id ?: item.displayName}" in favoriteFilters.value

    /** 收藏作品的 workId 集合（「收藏」页签客户端过滤用） */
    val favoriteWorkIds: StateFlow<Set<Int>> = repository.observeFavorites()
        .map { list -> list.map { it.workId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /**
     * 构建站点原生搜索语法（$ 包裹体系，已实测有效）：
     * `$tag:标签$`、`$-tag:标签$`（排除）、`$va:声优$`、`$circle:社团$`，直接与关键词拼接。
     * subtitle/order/sort 走独立查询参数。
     */
    fun buildQuery(keyword: String): String {
        val parts = mutableListOf<String>()
        if (keyword.isNotBlank()) parts += keyword.trim()

        val filters = mutableListOf<String>()
        _included.value[FilterType.TAG]?.forEach { filters += "\$tag:$it\$" }
        _excluded.value[FilterType.TAG]?.forEach { filters += "\$-tag:$it\$" }
        _included.value[FilterType.VA]?.forEach { filters += "\$va:$it\$" }
        _excluded.value[FilterType.VA]?.forEach { filters += "\$-va:$it\$" }
        _included.value[FilterType.CIRCLE]?.forEach { filters += "\$circle:$it\$" }
        _excluded.value[FilterType.CIRCLE]?.forEach { filters += "\$-circle:$it\$" }

        return (parts + filters).joinToString("")
    }

    fun hasActiveSelection(): Boolean =
        _included.value.any { it.value.isNotEmpty() } ||
            _excluded.value.any { it.value.isNotEmpty() } ||
            subtitleOnly.value

    fun reset() {
        _included.value = emptyMap()
        _excluded.value = emptyMap()
        excludeMode.value = false
        subtitleOnly.value = false
    }
}
