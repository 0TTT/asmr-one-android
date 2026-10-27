package com.asmrone.app.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asmrone.app.data.model.Work
import com.asmrone.app.data.repository.AsmrOneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页 / 搜索共用的列表状态。
 */
sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AsmrOneRepository,
) : ViewModel() {

    private val _works = MutableStateFlow<UiState<List<Work>>>(UiState.Loading)
    val works: StateFlow<UiState<List<Work>>> = _works.asStateFlow()

    private val _order = MutableStateFlow("create_date")
    val order: StateFlow<String> = _order.asStateFlow()

    private val _sort = MutableStateFlow("desc")

    private var currentPage = 1
    private var isLoadingMore = false

    init {
        loadWorks()
    }

    fun loadWorks() {
        viewModelScope.launch {
            _works.value = UiState.Loading
            currentPage = 1
            try {
                val resp = repository.getWorks(page = 1, order = _order.value, sort = _sort.value)
                _works.value = UiState.Success(resp.works)
            } catch (e: Exception) {
                _works.value = UiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore) return
        val current = _works.value
        if (current !is UiState.Success) return
        viewModelScope.launch {
            isLoadingMore = true
            try {
                val resp = repository.getWorks(page = currentPage + 1, order = _order.value, sort = _sort.value)
                currentPage++
                val merged = (current.data + resp.works).distinctBy { it.id }
                _works.value = UiState.Success(merged)
            } catch (_: Exception) {
                // 忽略加载更多失败
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun setOrder(order: String) {
        _order.value = order
        loadWorks()
    }

    fun setSort(sort: String) {
        _sort.value = sort
        loadWorks()
    }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: AsmrOneRepository,
    private val settingsRepository: com.asmrone.app.data.SettingsRepository,
) : ViewModel() {

    private val _results = MutableStateFlow<UiState<List<Work>>>(UiState.Loading)
    val results: StateFlow<UiState<List<Work>>> = _results.asStateFlow()

    fun search(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch { settingsRepository.addSearchHistory(keyword.trim()) }
        viewModelScope.launch {
            _results.value = UiState.Loading
            try {
                val resp = repository.searchWorks(keyword = keyword)
                _results.value = UiState.Success(resp.works)
            } catch (e: Exception) {
                _results.value = UiState.Error(e.message ?: "搜索失败")
            }
        }
    }
}
