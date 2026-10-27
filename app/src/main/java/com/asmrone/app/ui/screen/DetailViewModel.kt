package com.asmrone.app.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asmrone.app.data.model.TrackNode
import com.asmrone.app.data.model.Work
import com.asmrone.app.data.repository.AsmrOneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: AsmrOneRepository,
) : ViewModel() {

    private val _tracks = MutableStateFlow<UiState<List<TrackNode>>>(UiState.Loading)
    val tracks: StateFlow<UiState<List<TrackNode>>> = _tracks.asStateFlow()

    private var loadedWorkId: Int? = null

    /** 加载指定作品的音轨（若未加载过） */
    fun loadTracks(work: Work) {
        val id = work.id ?: return
        if (loadedWorkId == id) return
        loadedWorkId = id
        viewModelScope.launch {
            _tracks.value = UiState.Loading
            try {
                val nodes = repository.getTracks(id.toString())
                _tracks.value = UiState.Success(repository.flattenTracks(nodes))
            } catch (e: Exception) {
                _tracks.value = UiState.Error(e.message ?: "加载音轨失败")
            }
        }
    }

    fun observeFavorite(workId: Int): Flow<Boolean> = repository.observeIsFavorite(workId)

    fun toggleFavorite(work: Work) {
        viewModelScope.launch { repository.toggleFavorite(work) }
    }
}
