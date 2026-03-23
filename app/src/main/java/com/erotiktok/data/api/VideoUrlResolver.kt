package com.erotiktok.data.api

import com.erotiktok.data.model.VideoPageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 视频 URL 解析器
 * 从视频页面提取真实的视频地址
 */
class VideoUrlResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 解析视频页面获取真实视频 URL
     * @param videoPageUrl 视频页面URL
     * @return VideoPageData 包含视频信息的对象
     */
    suspend fun resolveVideoUrl(videoPageUrl: String): VideoPageData? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(videoPageUrl)
                .addHeader("Referer", "https://twitter-ero-video-ranking.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val html = response.body?.string() ?: return@withContext null
                return@withContext parseVideoFromHtml(html)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从 HTML 中解析视频信息
     */
    private fun parseVideoFromHtml(html: String): VideoPageData? {
        // 方法1: 尝试解析 application/ld+json
        val ldJsonPattern = """<script[^>]*type=["']application/ld\+json["'][^>]*>([\s\S]*?)</script>""".toRegex()
        ldJsonPattern.find(html)?.let { matchResult ->
            try {
                val jsonStr = matchResult.groupValues[1].trim()
                val json = JSONObject(jsonStr)

                if (json.has("contentUrl")) {
                    val videoUrl = json.getString("contentUrl")
                    val thumbnail = json.optString("thumbnailUrl")
                    val name = json.optString("name", "")
                    val description = json.optString("description", "")

                    // 尝试解析 duration
                    val duration = parseDuration(json.optString("duration"))

                    // 尝试解析上传者信息
                    val authorJson = json.optJSONObject("author")
                    val userName = authorJson?.optString("name") ?: ""
                    val userUrl = authorJson?.optString("url") ?: ""

                    return VideoPageData(
                        videoUrl = videoUrl,
                        title = name.ifEmpty { description },
                        thumbnail = thumbnail,
                        duration = duration,
                        views = 0,
                        likes = 0,
                        user = if (userName.isNotEmpty()) {
                            com.erotiktok.data.model.User(
                                id = 0,
                                name = userName,
                                screenName = userName,
                                avatar = "",
                                followersCount = 0,
                                followingCount = 0
                            )
                        } else null
                    )
                }

                // 嵌套的 VideoObject
                if (json.has("@type") && json.getString("@type") == "VideoObject") {
                    val videoUrl = json.optString("contentUrl")
                    if (videoUrl.isNotEmpty()) {
                        return VideoPageData(
                            videoUrl = videoUrl,
                            title = json.optString("name", ""),
                            thumbnail = json.optString("thumbnailUrl"),
                            duration = parseDuration(json.optString("duration")),
                            views = 0,
                            likes = 0,
                            user = null
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 方法2: 直接查找 video.twimg.com
        val videoTwimgPattern = """https?://video\.twimg\.com/[^"'<>\s]+\.mp4[^"'<>\s]*""".toRegex()
        videoTwimgPattern.find(html)?.let { matchResult ->
            return VideoPageData(
                videoUrl = matchResult.value,
                title = "",
                thumbnail = null,
                duration = 0,
                views = 0,
                likes = 0,
                user = null
            )
        }

        // 方法3: 查找 data-video-src
        val dataVideoSrcPattern = """data-video-src=["']([^"']+)["']""".toRegex()
        dataVideoSrcPattern.find(html)?.let { matchResult ->
            return VideoPageData(
                videoUrl = matchResult.groupValues[1],
                title = "",
                thumbnail = null,
                duration = 0,
                views = 0,
                likes = 0,
                user = null
            )
        }

        return null
    }

    /**
     * 解析 ISO 8601 duration 格式 (如 PT1M30S)
     */
    private fun parseDuration(duration: String): Int {
        if (duration.isEmpty()) return 0
        try {
            val pattern = """PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""".toRegex()
            val matchResult = pattern.find(duration)
            if (matchResult != null) {
                val hours = matchResult.groupValues[1].toIntOrNull() ?: 0
                val minutes = matchResult.groupValues[2].toIntOrNull() ?: 0
                val seconds = matchResult.groupValues[3].toIntOrNull() ?: 0
                return hours * 3600 + minutes * 60 + seconds
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }
}
