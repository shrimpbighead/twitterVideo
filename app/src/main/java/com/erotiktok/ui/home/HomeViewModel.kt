package com.erotiktok.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.erotiktok.data.model.Video
import com.erotiktok.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 视频分类标签
 */
enum class VideoTab(val title: String, val apiType: String) {
    NEW("最新", "new"),
    WEEK("周榜", "week"),
    MONTH("月榜", "month")
}

/**
 * HomeViewModel Factory - 用于创建带参数的 ViewModel
 */
class HomeViewModelFactory(private val tabType: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(tabType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * 首页 ViewModel - 每个Tab独立实例
 */
class HomeViewModel(private val tabType: String) : ViewModel() {
    val tabTypeName: String = tabType

    private val repository = VideoRepository.getInstance()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 滚动位置
    private val _firstVisibleItemIndex = MutableStateFlow(0)
    val firstVisibleItemIndex: StateFlow<Int> = _firstVisibleItemIndex.asStateFlow()

    private val _scrollOffset = MutableStateFlow(0)
    val scrollOffset: StateFlow<Int> = _scrollOffset.asStateFlow()

    // 初次加载标记
    private val _isFirstLoad = MutableStateFlow(true)
    val isFirstLoad: StateFlow<Boolean> = _isFirstLoad.asStateFlow()

    private var currentPage = 1

    init {
        Log.d("滚动位置", "=== HomeViewModel init called: tabType=$tabType, instance=$this ===")
        // 不在这里自动加载，等待 Tab 被选中时再加载
    }

    /**
     * 加载视频列表
     */
    fun loadVideos(refresh: Boolean = false) {
        Log.d("HomeViewModel", "loadVideos called: tabType=$tabType, refresh=$refresh, currentPage=$currentPage, isLoading=${_isLoading.value}")

        // 刷新时重置初次加载标记
        if (refresh) {
            _isFirstLoad.value = true
            Log.d("HomeViewModel", "refresh=true, isFirstLoad set to true: tabType=$tabType")
        }

        val isFirstPage = refresh || currentPage == 1
        Log.d("HomeViewModel", "loadVideos: tabType=$tabType, refresh=$refresh, isFirstPage=$isFirstPage")
        if (isFirstPage && _isLoading.value) {
            Log.d("HomeViewModel", "loadVideos skipped: already loading first page")
            return
        }
        if (!isFirstPage && _isLoadingMore.value) {
            Log.d("HomeViewModel", "loadVideos skipped: already loading more")
            return
        }

        viewModelScope.launch {
            try {
                val page = if (refresh) 1 else currentPage

                Log.d("HomeViewModel", "Loading videos: page=$page, tabType=$tabType")

                // 更新加载状态
                _isLoading.value = isFirstPage
                _isLoadingMore.value = !isFirstPage
                _isRefreshing.value = refresh
                _error.value = null

                // new用type参数，week/month用range参数
                val (type, range) = when (tabType) {
                    "new" -> "new" to null
                    "week" -> null to "weekly"
                    "month" -> null to "monthly"
                    else -> null to null
                }
                Log.d("HomeViewModel", "Calling repository.getVideoList: page=$page, type=$type, range=$range")
                val result = repository.getVideoList(page, range = range, type = type)

                result.fold(
                    onSuccess = { response ->
                        Log.d("HomeViewModel", "loadVideos SUCCESS: page=$page, items=${response.data?.size ?: 0}, totalPages=${response.totalPages}")
                        val responseData = response.data ?: emptyList()
                        val newVideos = if (refresh) responseData else (_videos.value + responseData).distinctBy { it.id }

                        _videos.value = newVideos
                        currentPage = page + 1
                        _hasMore.value = page < response.totalPages
                        _isLoading.value = false
                        _isLoadingMore.value = false
                        _isRefreshing.value = false
                        _isFirstLoad.value = false
                        Log.d("HomeViewModel", "isFirstLoad set to false: tabType=$tabType")
                        _error.value = null
                        Log.d("HomeViewModel", "After success: videos count = ${newVideos.size}, currentPage = $currentPage")
                    },
                    onFailure = { e ->
                        Log.e("HomeViewModel", "loadVideos FAILED: ${e.message}", e)
                        _isLoading.value = false
                        _isLoadingMore.value = false
                        _isRefreshing.value = false
                        _error.value = e.message ?: "加载失败"
                    }
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "loadVideos exception: ${e.message}", e)
                _isLoading.value = false
                _isLoadingMore.value = false
                _isRefreshing.value = false
                _error.value = e.message ?: "加载失败"
            }
        }
    }

    /**
     * 加载更多视频
     */
    fun loadMore() {
        if (!_hasMore.value || _isLoading.value || _isLoadingMore.value) return
        loadVideos()
    }

    /**
     * 刷新
     */
    fun refresh() {
        loadVideos(refresh = true)
    }

    /**
     * 重新加载
     */
    fun retry() {
        loadVideos(refresh = true)
    }

    /**
     * 保存滚动位置
     */
    fun saveScrollPosition(index: Int, offset: Int) {
        Log.d("滚动位置", "ViewModel saveScrollPosition: tabType=$tabType, index=$index, offset=$offset, currentValue=${_firstVisibleItemIndex.value}")
        _firstVisibleItemIndex.value = index
        _scrollOffset.value = offset
        Log.d("滚动位置", "ViewModel saveScrollPosition AFTER: tabType=$tabType, newValue=${_firstVisibleItemIndex.value}")
    }

    /**
     * 标记初次加载完成（当有缓存数据时调用）
     */
    fun markFirstLoadComplete() {
        _isFirstLoad.value = false
        Log.d("滚动位置", "markFirstLoadComplete: tabType=$tabType")
    }
}
