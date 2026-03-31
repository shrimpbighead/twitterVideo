package com.erotiktok.ui.player

import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erotiktok.data.model.Video
import com.erotiktok.ui.theme.PrimaryCyan
import kotlinx.coroutines.delay
import kotlin.math.abs
import androidx.activity.compose.BackHandler

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun TikTokPlayerScreen(
    videos: List<Video>,
    startIndex: Int = 0,
    allowBackgroundPlayback: Boolean,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showControls by remember { mutableStateOf(true) }

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(showControls, playerState.isPlaying) {
        if (playerState.isPlaying && showControls) {
            delay(3000)
            showControls = false
        }
    }

    // 处理系统返回手势
    BackHandler(enabled = true) {
        onBack()
    }

    DisposableEffect(lifecycleOwner, allowBackgroundPlayback) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !allowBackgroundPlayback && viewModel.playerState.value.isPlaying) {
                viewModel.pausePlayback()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 创建缓存和DataSource
    val cacheDir = context.cacheDir
    val cacheEvictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100MB缓存
    val simpleCache = remember(cacheDir) { SimpleCache(cacheDir, cacheEvictor) }

    // 释放SimpleCache
    DisposableEffect(Unit) {
        onDispose {
            simpleCache.release()
        }
    }

    val httpDataSourceFactory = remember {
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
    }

    val cacheDataSourceFactory = remember(simpleCache, httpDataSourceFactory) {
        CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    val exoPlayer = remember(cacheDataSourceFactory) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()
            .apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    LaunchedEffect(videos) {
        viewModel.setVideoList(videos, startIndex)
    }

    // 监听视频URL变化，准备播放器
    LaunchedEffect(playerState.currentVideoUrl) {
        playerState.currentVideoUrl?.let { url ->
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // 监听ExoPlayer状态，设置loading
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    viewModel.setLoading(false)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(playerState.isPlaying) {
        exoPlayer.playWhenReady = playerState.isPlaying
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.duration > 0) {
                viewModel.updateProgress(exoPlayer.currentPosition, exoPlayer.duration)
            }
            delay(100)
        }
    }

    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.onVideoEnded()
                }
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // 计算滑动进度 (0-1)
    val dragProgress = remember(dragOffset) {
        (dragOffset / 1000f).coerceIn(-1f, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragOffset = 0f
                    },
                    onDragEnd = {
                        isDragging = false
                        if (abs(dragOffset) > 100) {
                            if (dragOffset < 0) viewModel.playNext() else viewModel.playPrevious()
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = 0f
                    }
                ) { _, dragAmount -> dragOffset += dragAmount }
            }
    ) {
        // 主播放器（带滑动变换效果）
        VideoPlayer(
            exoPlayer = exoPlayer,
            thumbnail = playerState.currentVideo?.thumbnail,
            isLoading = playerState.isLoading,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 滑动时的缩放和透明度效果
                    scaleX = 1f - abs(dragProgress) * 0.1f
                    scaleY = 1f - abs(dragProgress) * 0.1f
                    alpha = 1f - abs(dragProgress) * 0.3f
                }
        )

        // 透明的点击层（不干扰Slider）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.togglePlayPause()
                    showControls = true
                }
        )

        // 加载指示器
        AnimatedVisibility(
            visible = playerState.isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(color = PrimaryCyan, modifier = Modifier.size(48.dp))
        }

        AnimatedVisibility(
            visible = showControls || !playerState.isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "关闭", tint = Color.White)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("${playerState.currentIndex + 1} / ${playerState.videoList.size}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // 右侧操作按钮（始终显示）
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            ActionButton(
                icon = Icons.Default.Download,
                count = 0,
                showCount = false,
                onClick = {
                    playerState.currentVideo?.let { video ->
                        downloadVideo(context, video, playerState.currentVideoUrl)
                    }
                }
            )
            ActionButtonWithSpeed(exoPlayer)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 进度条 - 使用最简单的实现
            Slider(
                value = playerState.progress.coerceIn(0f, 1f),
                onValueChange = { value ->
                    // 拖动时直接seek
                    val seekPos = (value * playerState.duration).toLong()
                    exoPlayer.seekTo(seekPos)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryCyan,
                    activeTrackColor = PrimaryCyan,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )

            // 时间显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${formatTime(playerState.currentPosition)} / ${formatTime(playerState.duration)}",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp
                )
            }
        }

        // 播放/暂停按钮移到屏幕中间
        AnimatedVisibility(
            visible = showControls || !playerState.isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (playerState.isPlaying) "暂停" else "播放",
                    tint = Color.White, modifier = Modifier.size(48.dp)
                )
            }
        }

        playerState.error?.let { error ->
            ErrorOverlay(error = error, onRetry = { viewModel.retry() }, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun ActionButtonWithSpeed(exoPlayer: ExoPlayer) {
    var expanded by remember { mutableStateOf(false) }
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val currentSpeed = exoPlayer.playbackParameters.speed

    Box {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.Speed, "播放速度", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Text("${currentSpeed}x", color = Color.White, fontSize = 12.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            speeds.forEach { speed ->
                DropdownMenuItem(
                    text = { Text("${speed}x", color = if (speed == currentSpeed) PrimaryCyan else Color.White) },
                    onClick = { exoPlayer.playbackParameters = PlaybackParameters(speed); expanded = false }
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    showCount: Boolean = true,
    isActive: Boolean = false,
    activeColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(icon, null, tint = if (isActive) activeColor else Color.White, modifier = Modifier.size(28.dp))
        }
        if (showCount && count > 0) Text(formatCount(count), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    thumbnail: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 视频加载中或切换视频时显示缩略图，避免切换瞬间黑屏
        if (thumbnail != null) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 暂停时显示模糊缩略图
        if (thumbnail != null && !isLoading && !exoPlayer.isPlaying) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
            )
        }

        // 播放时显示模糊背景
        if (thumbnail != null && !isLoading && exoPlayer.isPlaying) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(30.dp)
                    .alpha(0.3f)
            )
        }

        AndroidView(
            { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                    setKeepContentOnPlayerReset(true)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ErrorOverlay(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("加载失败", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(error, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        IconButton(onClick = onRetry, modifier = Modifier.background(PrimaryCyan, CircleShape)) {
            Icon(Icons.Default.PlayArrow, "重试", tint = Color.Black)
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

private fun downloadVideo(context: android.content.Context, video: Video, videoUrl: String?) {
    val url = videoUrl ?: video.url ?: run {
        android.widget.Toast.makeText(context, "无法获取视频链接", android.widget.Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
            .setTitle("下载视频")
            .setDescription("正在下载视频...")
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        val downloadId = downloadManager.enqueue(request)

        android.widget.Toast.makeText(context, "开始下载...", android.widget.Toast.LENGTH_SHORT).show()

        // 注册下载完成广播
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                val id = intent?.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val query = android.app.DownloadManager.Query().setFilterById(id)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)
                        val message = when (status) {
                            android.app.DownloadManager.STATUS_SUCCESSFUL -> "下载完成"
                            android.app.DownloadManager.STATUS_FAILED -> "下载失败"
                            else -> ""
                        }
                        if (message.isNotEmpty()) {
                            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        cursor.close()
                    }
                    try {
                        context?.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // Receiver 可能已经注销
                    }
                }
            }
        }

        context.registerReceiver(receiver, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE))

    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "下载失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}
