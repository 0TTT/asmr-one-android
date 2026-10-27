package com.asmrone.app.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asmrone.app.data.local.DownloadEntity
import com.asmrone.app.data.local.FavoriteEntity
import com.asmrone.app.data.local.PlayHistoryEntity
import com.asmrone.app.data.repository.AsmrOneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: AsmrOneRepository,
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteEntity>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<PlayHistoryEntity>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = repository.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun removeFavorite(workId: Int) {
        viewModelScope.launch { repository.removeFavorite(workId) }
    }

    fun removeDownload(url: String) {
        viewModelScope.launch { repository.removeDownload(url) }
    }
}
