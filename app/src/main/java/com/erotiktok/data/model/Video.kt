package com.erotiktok.data.model

import com.google.gson.annotations.SerializedName

/**
 * 视频数据模型 - 适配真实 API
 */
data class Video(
    val id: Long,
    val url: String?,
    @SerializedName("url_cd")
    val urlCd: String?,
    val thumbnail: String?,
    @SerializedName("time")
    val duration: Int = 0,
    @SerializedName("pv")
    val views: Int = 0,
    @SerializedName("favorite")
    val likes: Int = 0,
    @SerializedName("posted_at")
    val postedAt: String?,
    @SerializedName("tweet_url")
    val tweetUrl: String?,
    @SerializedName("tweet_account")
    val tweetAccount: String?,
    
    // UI 适配字段，处理可能的 null 值
    @SerializedName("title")
    private val _title: String? = null,
    val likeStatus: Boolean = false,
    val user: User? = null
) {
    // 安全获取标题，如果为 null 则返回空字符串或默认文案
    val title: String
        get() = _title ?: tweetAccount ?: "Video #$id"
}

/**
 * 用户数据模型
 */
data class User(
    val id: Long,
    val name: String?,
    @SerializedName("screen_name")
    val screenName: String?,
    val avatar: String?,
    @SerializedName("followers_count")
    val followersCount: Int = 0,
    @SerializedName("following_count")
    val followingCount: Int = 0
)

/**
 * API 响应模型
 */
data class VideoListResponse(
    @SerializedName("items")
    val data: List<Video>?,
    @SerializedName("currentPage")
    val page: Int = 1,
    @SerializedName("lastPage")
    val totalPages: Int = 1,
    @SerializedName("total")
    val totalCount: Int = 0
)

/**
 * 单个视频详情响应
 */
data class VideoDetailResponse(
    val success: Boolean,
    val data: Video?
)

/**
 * 点赞响应
 */
data class LikeResponse(
    val success: Boolean,
    @SerializedName("like_status")
    val likeStatus: Boolean,
    val likes: Int
)

/**
 * 视频详情页解析模型
 */
data class VideoPageData(
    val videoUrl: String?,
    val title: String?,
    val thumbnail: String?,
    val duration: Int,
    val views: Int,
    val likes: Int,
    val user: User?
)
