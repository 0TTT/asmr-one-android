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

/** 文件树展平后的一行：深度（缩进）+ 节点 + 父文件夹（用于"同文件夹音频成队"） */
data class TreeRow(
    val depth: Int,
    val node: TrackNode,
    val parent: TrackNode?,
)

/** 全树展平（全部展开，保持顺序） */
fun flattenTree(nodes: List<TrackNode>): List<TreeRow> {
    val out = mutableListOf<TreeRow>()
    fun walk(list: List<TrackNode>, depth: Int, parent: TrackNode?) {
        for (n in list) {
            out.add(TreeRow(depth, n, parent))
            if (n.children.isNotEmpty()) walk(n.children, depth + 1, n)
        }
    }
    walk(nodes, 0, null)
    return out
}

/** 收集某个文件夹下（递归）所有文件节点的 hash */
fun collectFileHashes(folder: TrackNode): Set<String> {
    val out = mutableSetOf<String>()
    fun walk(n: TrackNode) {
        n.children.forEach { c ->
            c.hash?.let { out.add(it) }
            walk(c)
        }
    }
    walk(folder)
    return out
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: AsmrOneRepository,
) : ViewModel() {

    /** 作品文件树（原样展示：文件夹/音频/字幕/图片/文本等） */
    private val _tree = MutableStateFlow<UiState<List<TrackNode>>>(UiState.Loading)
    val tree: StateFlow<UiState<List<TrackNode>>> = _tree.asStateFlow()

    private var loadedWorkId: Int? = null

    /** 加载指定作品的文件树（若未加载过） */
    fun loadTracks(work: Work) {
        val id = work.id ?: return
        if (loadedWorkId == id) return
        loadedWorkId = id
        viewModelScope.launch {
            _tree.value = UiState.Loading
            try {
                val nodes = repository.getTracks(id.toString())
                _tree.value = UiState.Success(nodes)
            } catch (e: Exception) {
                _tree.value = UiState.Error(e.message ?: "加载文件列表失败")
            }
        }
    }

    fun observeFavorite(workId: Int): Flow<Boolean> = repository.observeIsFavorite(workId)

    fun toggleFavorite(work: Work) {
        viewModelScope.launch { repository.toggleFavorite(work) }
    }
}
