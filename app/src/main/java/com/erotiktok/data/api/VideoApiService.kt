package com.erotiktok.data.api

import com.erotiktok.data.model.LikeResponse
import com.erotiktok.data.model.VideoDetailResponse
import com.erotiktok.data.model.VideoListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 点赞请求体
 */
data class LikeRequest(
    val favorite: Int
)

/**
 * 视频 API 服务
 */
interface VideoApiService {

    /**
     * 获取视频列表 - 最新/周榜/月榜
     * @param page 页码
     * @param type 类型: "new"(最新)
     * @param range 类型: "week"(周榜), "month"(月榜)
     */
    @GET("api/media")
    suspend fun getVideoList(
        @Query("page") page: Int = 1,
        @Query("type") type: String? = null,
        @Query("range") range: String? = null
    ): Response<VideoListResponse>

    /**
     * 获取单个视频详情
     */
    @GET("api/media/{id}")
    suspend fun getVideoDetail(
        @Path("id") movieId: Long
    ): Response<VideoDetailResponse>

    /**
     * 点赞/取消点赞
     * @param movieId 视频ID
     * @param request 点赞请求 (favorite: 1=点赞, 0=取消)
     */
    @POST("api/media/{id}/favorite")
    suspend fun likeVideo(
        @Path("id") movieId: Long,
        @Body request: LikeRequest
    ): Response<LikeResponse>
}
