package com.erotiktok.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erotiktok.data.model.Video
import com.erotiktok.ui.home.HomeScreen
import com.erotiktok.ui.home.HomeViewModel
import com.erotiktok.ui.home.HomeViewModelFactory
import com.erotiktok.ui.home.VideoTab
import com.erotiktok.ui.player.TikTokPlayerScreen

/**
 * 导航主机 - 使用状态控制代替导航
 */
@Composable
fun EroTikHost() {
    println("===== [导航] EroTikHost 开始执行 =====")

    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var playerVideos by rememberSaveable { mutableStateOf<List<Video>>(emptyList()) }
    var playerStartIndex by rememberSaveable { mutableIntStateOf(0) }

    // 提升 Tab 状态到导航层，确保播放页返回时保持状态
    var selectedTab by rememberSaveable { mutableStateOf(VideoTab.NEW) }

    // 提升 ViewModel 到导航层，确保播放页返回时不被重新创建
    val viewModelNEW: HomeViewModel = viewModel(factory = HomeViewModelFactory("new"), key = VideoTab.NEW.name)
    val viewModelWEEK: HomeViewModel = viewModel(factory = HomeViewModelFactory("week"), key = VideoTab.WEEK.name)
    val viewModelMONTH: HomeViewModel = viewModel(factory = HomeViewModelFactory("month"), key = VideoTab.MONTH.name)

    println("[导航] showPlayer = $showPlayer")
    println("[导航] playerVideos 数量 = ${playerVideos.size}")
    println("[导航] selectedTab = $selectedTab")

    if (showPlayer) {
        println("[导航] 显示播放器页面")
        TikTokPlayerScreen(
            videos = playerVideos,
            startIndex = playerStartIndex,
            onBack = {
                println("[导航] 点击返回按钮，设置 showPlayer = false")
                showPlayer = false
            }
        )
    } else {
        println("[导航] 显示首页列表")
        HomeScreen(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            viewModelNEW = viewModelNEW,
            viewModelWEEK = viewModelWEEK,
            viewModelMONTH = viewModelMONTH,
            onVideoClick = { videos, startIndex ->
                println("[导航] 点击视频: videos=${videos.size}, startIndex=$startIndex")
                playerVideos = videos
                playerStartIndex = startIndex
                showPlayer = true
            },
            onSaveScrollPosition = { index, offset ->
                // Tab 切换时保存当前 ViewModel 的滚动位置
                val currentViewModel = when (selectedTab) {
                    VideoTab.NEW -> viewModelNEW
                    VideoTab.WEEK -> viewModelWEEK
                    VideoTab.MONTH -> viewModelMONTH
                }
                println("[滚动位置] 导航 保存: tab=$selectedTab, index=$index, offset=$offset, viewModel=$currentViewModel")
                currentViewModel.saveScrollPosition(index, offset)
            }
        )
    }
    println("===== [导航] EroTikHost 执行完毕 =====")
}
