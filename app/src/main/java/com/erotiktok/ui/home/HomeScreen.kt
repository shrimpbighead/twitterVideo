package com.erotiktok.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.erotiktok.data.model.Video
import com.erotiktok.ui.theme.BackgroundBlack
import com.erotiktok.ui.theme.PrimaryCyan
import com.erotiktok.ui.theme.SurfaceDark

fun println(message: String) {
    Log.d("APP", message)
}

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    selectedTab: VideoTab,
    onTabSelected: (VideoTab) -> Unit,
    viewModelNEW: HomeViewModel,
    viewModelWEEK: HomeViewModel,
    viewModelMONTH: HomeViewModel,
    allowBackgroundPlayback: Boolean,
    onAllowBackgroundPlaybackChange: (Boolean) -> Unit,
    onVideoClick: (List<Video>, Int) -> Unit,
    onSaveScrollPosition: (Int, Int) -> Unit
) {
    println("===== [首页] HomeScreen 开始渲染 =====")

    // 为每个 Tab 创建独立的 GridState，确保滚动位置互不影响
    val gridStateNEW = rememberLazyGridState()
    val gridStateWEEK = rememberLazyGridState()
    val gridStateMONTH = rememberLazyGridState()

    // 获取当前 Tab 对应的 ViewModel 和 GridState
    val currentViewModel = when (selectedTab) {
        VideoTab.NEW -> viewModelNEW
        VideoTab.WEEK -> viewModelWEEK
        VideoTab.MONTH -> viewModelMONTH
    }
    val currentGridState = when (selectedTab) {
        VideoTab.NEW -> gridStateNEW
        VideoTab.WEEK -> gridStateWEEK
        VideoTab.MONTH -> gridStateMONTH
    }

    println("[首页] selectedTab=$selectedTab, currentViewModel=$currentViewModel")

    val tabs = VideoTab.entries
    val selectedTabIndex = tabs.indexOf(selectedTab)
    val density = LocalDensity.current
    val tabSwipeThresholdPx = remember(density) { with(density) { 64.dp.toPx() } }

    fun switchToTab(targetIndex: Int) {
        if (targetIndex == selectedTabIndex || targetIndex !in tabs.indices) {
            return
        }

        val saveIndex = currentGridState.firstVisibleItemIndex
        val saveOffset = currentGridState.firstVisibleItemScrollOffset
        val targetTab = tabs[targetIndex]
        println("[滚动位置] HomeScreen Tab切换: $selectedTab -> $targetTab, 保存位置: index=$saveIndex, offset=$saveOffset")
        onSaveScrollPosition(saveIndex, saveOffset)
        onTabSelected(targetTab)
    }

    var horizontalDragOffset by remember(selectedTab) { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .pointerInput(selectedTab) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDragOffset += dragAmount
                    },
                    onDragEnd = {
                        when {
                            horizontalDragOffset > tabSwipeThresholdPx -> switchToTab(selectedTabIndex - 1)
                            horizontalDragOffset < -tabSwipeThresholdPx -> switchToTab(selectedTabIndex + 1)
                        }
                        horizontalDragOffset = 0f
                    },
                    onDragCancel = {
                        horizontalDragOffset = 0f
                    }
                )
            }
    ) {
        Spacer(
            modifier = Modifier
                .statusBarsPadding()
                .background(SurfaceDark)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "后台播放",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = allowBackgroundPlayback,
                onCheckedChange = onAllowBackgroundPlaybackChange
            )
        }

        // 分类标签
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = SurfaceDark,
            contentColor = Color.White,
            divider = {}
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { switchToTab(index) },
                    modifier = Modifier.weight(1f),
                    text = {
                        Text(
                            text = tab.title,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == tab) PrimaryCyan else Color.White.copy(alpha = 0.7f)
                        )
                    }
                )
            }
        }

        // Tab 内容 - 切换 Tab 时重建收集状态，避免上一 Tab 的瞬时旧数据串到当前 Tab
        key(selectedTab) {
            val currentVideos = currentViewModel.videos.collectAsState().value
            val isLoading = currentViewModel.isLoading.collectAsState().value
            val isLoadingMore = currentViewModel.isLoadingMore.collectAsState().value
            val hasMore = currentViewModel.hasMore.collectAsState().value
            val isRefreshing = currentViewModel.isRefreshing.collectAsState().value
            val error = currentViewModel.error.collectAsState().value

            println("[HomeScreen] 传递给 TabContent: selectedTab=$selectedTab, viewModel.tabType=${currentViewModel.tabTypeName}, videos.size=${currentVideos.size}")
            TabContent(
                viewModel = currentViewModel,
                gridState = currentGridState,
                viewModelKey = selectedTab.name,
                videos = currentVideos,
                isLoading = isLoading,
                isLoadingMore = isLoadingMore,
                hasMore = hasMore,
                isRefreshing = isRefreshing,
                error = error,
                onVideoClick = { videos, index ->
                    val saveIndex = currentGridState.firstVisibleItemIndex
                    val saveOffset = currentGridState.firstVisibleItemScrollOffset
                    println("[滚动位置] HomeScreen 点击视频: tab=$selectedTab, 保存位置: index=$saveIndex, offset=$saveOffset")
                    onSaveScrollPosition(saveIndex, saveOffset)
                    onVideoClick(videos, index)
                }
            )
        }
    }
    println("===== [首页] HomeScreen 渲染完毕 =====")
}

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun TabContent(
    viewModel: HomeViewModel,
    gridState: LazyGridState,
    viewModelKey: String,
    videos: List<Video>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    isRefreshing: Boolean,
    error: String?,
    onVideoClick: (List<Video>, Int) -> Unit
) {
    // 使用 viewModelKey 强制重新订阅 StateFlow
    remember(viewModelKey) { viewModel }
    // GridState 由外部传入，每个 Tab 独立保持
    // val gridState = rememberLazyGridState()  // 删除这行

    // 首次显示时加载数据
    LaunchedEffect(viewModelKey) {
        println("[滚动位置] LaunchedEffect viewModelKey=$viewModelKey, videos.size=${videos.size}, isFirstLoad=${viewModel.isFirstLoad.value}")
        if (videos.isEmpty()) {
            println("[滚动位置] 调用 loadVideos: $viewModelKey")
            viewModel.loadVideos()
        } else if (viewModel.isFirstLoad.value) {
            // 如果有缓存数据但 isFirstLoad 为 true，需要重置为 false
            println("[滚动位置] 调用 markFirstLoadComplete: $viewModelKey")
            viewModel.markFirstLoadComplete()
        }
    }

    println("[TabContent] viewModel=$viewModel, videosCount=${videos.size}")

    // 使用 key 确保 ViewModel 切换时强制重新订阅 StateFlow
    // 立即读取当前值而不是等待 collectAsState
    val savedIndex = viewModel.firstVisibleItemIndex.value
    val savedOffset = viewModel.scrollOffset.value
    val isFirstLoad = viewModel.isFirstLoad.value

    println("[滚动位置] TabContent: viewModel.tabType=${viewModel.tabTypeName}, isFirstLoad=$isFirstLoad, savedIndex=$savedIndex, savedOffset=$savedOffset")

    // 只在当前 Tab 首次显示出列表内容时恢复一次滚动位置，避免加载更多后跳回旧位置
    var hasAppliedInitialScroll by remember(viewModelKey) { mutableStateOf(false) }

    LaunchedEffect(viewModelKey, videos.isNotEmpty(), isFirstLoad) {
        if (hasAppliedInitialScroll || videos.isEmpty()) {
            return@LaunchedEffect
        }

        // 直接从 StateFlow 读取当前值
        val currentSavedIndex = viewModel.firstVisibleItemIndex.value
        val currentSavedOffset = viewModel.scrollOffset.value
        val currentIsFirstLoad = viewModel.isFirstLoad.value

        println("[滚动位置] 初次应用滚动位置: isFirstLoad=$currentIsFirstLoad, savedIndex=$currentSavedIndex, savedOffset=$currentSavedOffset")
        if (currentIsFirstLoad) {
            // 初次加载，滚动到顶部
            println("[滚动位置] 滚动到顶部")
            gridState.scrollToItem(0, 0)
        } else if (currentSavedIndex > 0 || currentSavedOffset > 0) {
            // 非初次加载，恢复之前的位置
            println("[滚动位置] 恢复位置: index=$currentSavedIndex, offset=$currentSavedOffset")
            gridState.scrollToItem(currentSavedIndex, currentSavedOffset)
        }

        hasAppliedInitialScroll = true
    }

    // 底部加载检测
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
            totalItems > 0 && lastVisible != null && lastVisible.index >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && hasMore && !isLoading && !isLoadingMore) {
            viewModel.loadMore()
        }
    }

    val showPullRefreshIndicator = isRefreshing && videos.isNotEmpty()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = showPullRefreshIndicator,
        onRefresh = { viewModel.refresh() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (isLoading && videos.isEmpty()) {
            println("[TabContent] 显示加载中...")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryCyan)
            }
        } else if (error != null && videos.isEmpty()) {
            println("[TabContent] 显示错误: $error")
            ErrorContent(
                error = error,
                onRetry = { viewModel.retry() }
            )
        } else {
            println("[TabContent] 显示视频列表, videos数量=${videos.size}")
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = videos,
                    key = { it.id }
                ) { video ->
                    VideoCard(
                        video = video,
                        onClick = {
                            val index = videos.indexOf(video)
                            println("[TabContent] 点击视频: index=$index, title=${video.title}")
                            onVideoClick(videos, index)
                        }
                    )
                }

                // 底部加载指示器
                if ((isLoading || isLoadingMore) && videos.isNotEmpty()) {
                    println("[TabContent] 显示底部加载中...")
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showPullRefreshIndicator) {
            PullRefreshIndicator(
                refreshing = true,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = SurfaceDark,
                contentColor = PrimaryCyan
            )
        }
    }
}

/**
 * 视频卡片组件
 */
@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .clickable(onClick = onClick)
    ) {
        // 缩略图
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .background(Color.DarkGray)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnail)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            ),
                            startY = 200f
                        )
                    )
            )

            // 播放图标和时长
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDuration(video.duration),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 点赞状态
            if (video.likeStatus) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            PrimaryCyan.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Liked",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 视频信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户头像
            AsyncImage(
                model = video.user?.avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.Gray, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 标题和用户
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${video.user?.screenName ?: "unknown"}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }

        // 视频链接
        val videoUrl = video.url ?: video.urlCd
        if (!videoUrl.isNullOrBlank()) {
            val fullUrl = videoUrl
            val pathPart = try {
                java.net.URI(fullUrl).path ?: fullUrl
            } catch (e: Exception) {
                fullUrl
            }

            val context = LocalContext.current

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "URL", annotation = fullUrl)
                    withStyle(style = SpanStyle(color = PrimaryCyan, fontSize = 10.sp)) {
                        append(pathPart)
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                try {
                                    uriHandler.openUri(annotation.item)
                                } catch (e: Exception) {
                                    // 忽略
                                }
                            }
                    }
                )
                // 复制按钮
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Video URL", fullUrl)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "已复制链接", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制链接",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 错误内容
 */
@Composable
fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "加载失败",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        IconButton(
            onClick = onRetry,
            modifier = Modifier
                .background(PrimaryCyan, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "重试",
                tint = Color.Black
            )
        }
    }
}

/**
 * 格式化时长
 */
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}
