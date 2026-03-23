package com.erotiktok.data.repository

import android.util.Log
import com.erotiktok.data.api.ApiClient
import com.erotiktok.data.api.LikeRequest
import com.erotiktok.data.api.VideoUrlResolver
import com.erotiktok.data.model.Video
import com.erotiktok.data.model.VideoListResponse
import com.erotiktok.data.model.VideoPageData

/**
 * 视频仓库
 */
class VideoRepository {

    private val apiService = ApiClient.videoApiService
    private val videoUrlResolver = VideoUrlResolver()

    /**
     * 获取视频列表 - 最新/周榜/月榜都使用这个接口
     * @param page 页码
     * @param range 类型: "week"(周榜), "month"(月榜)，最新时传null
     * @param type 类型: "new"(最新)，周榜月榜时传null
     */
    suspend fun getVideoList(page: Int = 1, range: String? = null, type: String? = null): Result<VideoListResponse> {
        Log.d("VideoRepository", "getVideoList: page=$page, type=$type, range=$range")
        return try {
            val response = apiService.getVideoList(page, type, range)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d("VideoRepository", "getVideoList success: ${body.data ?: 0} items")
                Result.success(body)
            } else {
                val errorMsg = "Failed to fetch videos: ${response.code()} ${response.message()}"
                Log.e("VideoRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "getVideoList exception", e)
            Result.failure(e)
        }
    }

    /**
     * 解析视频真实URL
     */
    suspend fun resolveVideoUrl(videoPageUrl: String): Result<VideoPageData> {
        return try {
            val result = videoUrlResolver.resolveVideoUrl(videoPageUrl)
            if (result != null && result.videoUrl != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Could not resolve video URL"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 点赞/取消点赞视频
     * @param movieId 视频ID
     * @param isLiked 当前点赞状态，true=点赞，false=取消点赞
     */
    suspend fun likeVideo(movieId: Long, isLiked: Boolean): Result<Boolean> {
        return try {
            val request = LikeRequest(if (isLiked) 1 else 0)
            val response = apiService.likeVideo(movieId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.likeStatus)
            } else {
                Result.failure(Exception("Failed to like video: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var instance: VideoRepository? = null

        fun getInstance(): VideoRepository {
            return instance ?: synchronized(this) {
                instance ?: VideoRepository().also { instance = it }
            }
        }
    }
}
