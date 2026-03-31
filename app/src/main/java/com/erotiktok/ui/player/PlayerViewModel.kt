package com.erotiktok.ui.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erotiktok.data.model.Video
import com.erotiktok.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 视频播放器状态
 */
data class PlayerState(
    val isLoading: Boolean = true,
    val isPlaying: Boolean = true,
    val isMuted: Boolean = false,
    val currentVideo: Video? = null,
    val currentIndex: Int = 0,
    val videoList: List<Video> = emptyList(),
    val error: String? = null,
    val currentVideoUrl: String? = null,
    val nextVideoUrl: String? = null,  // 预加载下一个视频URL
    val progress: Float = 0f,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val loopEnabled: Boolean = true
)

/**
 * 视频播放器 ViewModel
 */
class PlayerViewModel : ViewModel() {

    private val repository = VideoRepository.getInstance()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    /**
     * 设置视频列表
     */
    fun setVideoList(videos: List<Video>, startIndex: Int = 0) {
        _playerState.value = _playerState.value.copy(
            videoList = videos,
            currentIndex = startIndex,
            currentVideo = videos.getOrNull(startIndex),
            isLoading = true,
            error = null,
            isPlaying = true,  // 默认自动播放
            progress = 0f,
            currentPosition = 0L
        )
        // 解析第一个视频的 URL
        resolveCurrentVideoUrl()
    }

    /**
     * 追加视频列表（用于加载更多后同步更新）
     */
    fun appendVideos(newVideos: List<Video>) {
        val currentList = _playerState.value.videoList.toMutableList()
        // 避免重复添加
        val existingIds = currentList.map { it.id }.toSet()
        val videosToAdd = newVideos.filter { it.id !in existingIds }
        currentList.addAll(videosToAdd)

        _playerState.value = _playerState.value.copy(videoList = currentList)
    }

    /**
     * 解析当前视频的真实 URL
     */
    private fun resolveCurrentVideoUrl() {
        val currentVideo = _playerState.value.currentVideo ?: return
        val videoUrl = currentVideo.url ?: return

        viewModelScope.launch {
            _playerState.value = _playerState.value.copy(isLoading = true, error = null)

            // 如果 url 是完整的 mp4，直接使用；如果不是，再考虑解析
            if (videoUrl.endsWith(".mp4") || videoUrl.contains(".mp4?")) {
                _playerState.value = _playerState.value.copy(
                    currentVideoUrl = videoUrl,
                    error = null
                )
                // 预加载下一个视频
                preloadNextVideo()
            } else {
                val result = repository.resolveVideoUrl(videoUrl)
                result.fold(
                    onSuccess = { pageData ->
                        _playerState.value = _playerState.value.copy(
                            currentVideoUrl = pageData.videoUrl,
                            error = null
                        )
                        // 预加载下一个视频
                        preloadNextVideo()
                    },
                    onFailure = { e ->
                        _playerState.value = _playerState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load video"
                        )
                    }
                )
            }
        }
    }

    /**
     * 预加载下一个视频的 URL
     */
    private fun preloadNextVideo() {
        val state = _playerState.value
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.videoList.size) return

        val nextVideo = state.videoList.getOrNull(nextIndex) ?: return
        val nextVideoUrl = nextVideo.url ?: return

        viewModelScope.launch {
            // 如果已经是mp4，直接使用
            if (nextVideoUrl.endsWith(".mp4") || nextVideoUrl.contains(".mp4?")) {
                _playerState.value = _playerState.value.copy(nextVideoUrl = nextVideoUrl)
            } else {
                val result = repository.resolveVideoUrl(nextVideoUrl)
                result.onSuccess { pageData ->
                    _playerState.value = _playerState.value.copy(nextVideoUrl = pageData.videoUrl)
                }
            }
        }
    }

    /**
     * 切换到下一个视频
     */
    fun playNext() {
        val state = _playerState.value
        if (state.currentIndex < state.videoList.size - 1) {
            val newIndex = state.currentIndex + 1

            // 获取下一个视频的缩略图
            val nextVideo = state.videoList.getOrNull(newIndex)

            // 如果有预加载的URL，直接使用
            val preloadUrl = if (state.currentIndex + 2 < state.videoList.size) {
                state.nextVideoUrl
            } else null
            if(preloadUrl != null){
                Log.d("缩略图url",preloadUrl);
            }


            // 先更新视频，显示缩略图
            _playerState.value = state.copy(
                currentIndex = newIndex,
                currentVideo = nextVideo,
                isLoading = true,
                nextVideoUrl = null,
                progress = 0f,
                currentPosition = 0L,
                isPlaying = true
            )

            // 设置视频URL（延迟设置loading为false）
            if (preloadUrl != null) {
                _playerState.value = _playerState.value.copy(
                    currentVideoUrl = preloadUrl
                )
                preloadNextVideo()
            } else {
                resolveCurrentVideoUrl()
            }
        }
    }

    /**
     * 切换到上一个视频
     */
    fun playPrevious() {
        val state = _playerState.value
        if (state.currentIndex > 0) {
            val newIndex = state.currentIndex - 1
            _playerState.value = state.copy(
                currentIndex = newIndex,
                currentVideo = state.videoList[newIndex],
                isLoading = true,
                progress = 0f,
                currentPosition = 0L,
                isPlaying = true
            )
            resolveCurrentVideoUrl()
        }
    }

    /**
     * 跳转到指定索引
     */
    fun seekTo(index: Int) {
        val state = _playerState.value
        if (index in state.videoList.indices && index != state.currentIndex) {
            _playerState.value = state.copy(
                currentIndex = index,
                currentVideo = state.videoList[index],
                isLoading = true,
                progress = 0f,
                currentPosition = 0L
            )
            resolveCurrentVideoUrl()
        }
    }

    /**
     * 设置loading状态
     */
    fun setLoading(loading: Boolean) {
        _playerState.value = _playerState.value.copy(isLoading = loading)
    }

    /**
     * 切换播放/暂停
     */
    fun togglePlayPause() {
        _playerState.value = _playerState.value.copy(
            isPlaying = !_playerState.value.isPlaying
        )
    }

    fun pausePlayback() {
        _playerState.value = _playerState.value.copy(isPlaying = false)
    }

    /**
     * 切换静音
     */
    fun toggleMute() {
        _playerState.value = _playerState.value.copy(
            isMuted = !_playerState.value.isMuted
        )
    }

    /**
     * 设置静音状态
     */
    fun setMuted(muted: Boolean) {
        _playerState.value = _playerState.value.copy(isMuted = muted)
    }

    /**
     * 切换循环播放
     */
    fun toggleLoop() {
        _playerState.value = _playerState.value.copy(
            loopEnabled = !_playerState.value.loopEnabled
        )
    }

    /**
     * 更新播放进度
     */
    fun updateProgress(position: Long, duration: Long) {
        val progress = if (duration > 0) position.toFloat() / duration else 0f
        _playerState.value = _playerState.value.copy(
            progress = progress,
            currentPosition = position,
            duration = duration
        )
    }

    /**
     * 视频播放完成
     */
    fun onVideoEnded() {
        if (_playerState.value.loopEnabled) {
            _playerState.value = _playerState.value.copy(
                isPlaying = true,
                progress = 0f,
                currentPosition = 0L
            )
        } else {
            playNext()
        }
    }

    /**
     * 点赞/取消点赞当前视频
     */
    fun likeCurrentVideo() {
        val video = _playerState.value.currentVideo ?: return

        viewModelScope.launch {
            // 切换点赞状态：如果当前是点赞的，则取消；否则点赞
            val newLikedState = !video.likeStatus

            val result = repository.likeVideo(video.id, newLikedState)
            result.fold(
                onSuccess = { isLiked ->
                    val updatedVideo = video.copy(
                        likeStatus = isLiked
                    )
                    val updatedList = _playerState.value.videoList.toMutableList()
                    updatedList[_playerState.value.currentIndex] = updatedVideo

                    _playerState.value = _playerState.value.copy(
                        currentVideo = updatedVideo,
                        videoList = updatedList
                    )
                },
                onFailure = { /* Handle error */ }
            )
        }
    }

    /**
     * 重新加载当前视频
     */
    fun retry() {
        resolveCurrentVideoUrl()
    }
}
