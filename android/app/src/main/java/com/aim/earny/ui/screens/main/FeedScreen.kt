@file:OptIn(ExperimentalFoundationApi::class, UnstableApi::class)

package com.aim.earny.ui.screens.main

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aim.earny.BuildConfig
import com.aim.earny.data.AppEvents
import com.aim.earny.data.Video
import com.aim.earny.data.formatCount
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.FeedViewModel

/**
 * 🎯 THE MAGIC: Custom DataSource that forces ExoPlayer
 * to always send open-ended Range requests (bytes=N-)
 * instead of small bounded ranges (bytes=N-M).
 *
 * This bypasses Cloudflare's 502 on small ranges entirely.
 */
@UnstableApi
private fun buildCloudflareSafeDataSourceFactory(context: Context): DataSource.Factory {
    val baseFactory = DefaultDataSource.Factory(context)
    // Anonymous object — SAM conversion with default methods can be flaky
    val resolver = object : ResolvingDataSource.Resolver {
        override fun resolveDataSpec(dataSpec: androidx.media3.datasource.DataSpec): androidx.media3.datasource.DataSpec {
            // Keep same position, remove the length cap.
            // Converts "Range: bytes=N-M" into "Range: bytes=N-"
            // which Cloudflare accepts unconditionally.
            return dataSpec.subrange(0L, C.LENGTH_UNSET.toLong())
        }
    }
    return ResolvingDataSource.Factory(baseFactory, resolver)
}

@Composable
fun FeedScreen(vm: FeedViewModel = viewModel()) {
    val videos by vm.videos.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val likedIds by vm.likedIds.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(1) }

    val feedRefresh by AppEvents.feedRefresh.collectAsStateWithLifecycle()
    LaunchedEffect(feedRefresh) { vm.load() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            loading && videos.isEmpty() -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Gold) }

            error != null && videos.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Couldn't load feed", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(error!!, color = TextWhite60, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.load() }) { Text("Retry") }
            }

            videos.isEmpty() -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.Videocam, null,
                    tint = Gold.copy(alpha = 0.7f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("No videos yet", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap the Earny Orb to post the first one",
                    color = TextWhite60, fontSize = 13.sp
                )
            }

            else -> {
                val pager = rememberPagerState(pageCount = { videos.size })
                val currentIndex = pager.currentPage
                val ctx = LocalContext.current

                // ---- SINGLE ExoPlayer with Cloudflare-safe DataSource ----
                val exo = remember {
                    val safeFactory = buildCloudflareSafeDataSourceFactory(ctx)
                    val mediaSourceFactory = DefaultMediaSourceFactory(safeFactory)
                    ExoPlayer.Builder(ctx)
                        .setMediaSourceFactory(mediaSourceFactory)
                        .build()
                        .apply {
                            repeatMode = ExoPlayer.REPEAT_MODE_ONE
                            playWhenReady = true
                        }
                }

                DisposableEffect(Unit) {
                    onDispose { exo.release() }
                }

                LaunchedEffect(currentIndex, videos.size) {
                    if (videos.isNotEmpty() && currentIndex in videos.indices) {
                        val v = videos[currentIndex]
                        if (v.telegramMsgId > 0) {
                            val url = BuildConfig.API_BASE.trimEnd('/') + "/stream/" + v.telegramMsgId
                            exo.setMediaItem(MediaItem.fromUri(url))
                            exo.prepare()
                            exo.play()
                        }
                    }
                }

                VerticalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val v = videos[page]
                    VideoPage(
                        video = v,
                        isCurrentPage = page == currentIndex,
                        exo = exo,
                        isLiked = likedIds.contains(v.id),
                        onToggleLike = { vm.toggleLike(v) },
                        onBecameVisible = { vm.onPageVisible(v) }
                    )
                }
            }
        }

        // Top tabs
        Row(
            Modifier.fillMaxWidth().padding(top = 40.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            Tab("Following", tab == 0) { tab = 0 }
            Spacer(Modifier.width(20.dp))
            Tab("For You", tab == 1) { tab = 1 }
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Filled.Search, "search",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun Tab(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text,
            color = if (selected) Color.White else TextWhite60,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.width(if (selected) 28.dp else 0.dp).height(2.dp)
                .background(if (selected) Color.White else Color.Transparent)
        )
    }
}

@Composable
private fun VideoPage(
    video: Video,
    isCurrentPage: Boolean,
    exo: ExoPlayer,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onBecameVisible: () -> Unit
) {
    var following by remember { mutableStateOf(false) }
    var burstKey by remember { mutableStateOf(0) }
    val ctx = LocalContext.current

    // Notify VM when this page becomes the active one
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) onBecameVisible()
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(video.id, isCurrentPage) {
                if (!isCurrentPage) return@pointerInput
                detectTapGestures(
                    onDoubleTap = {
                        if (!isLiked) onToggleLike()
                        burstKey++
                    },
                    onTap = {
                        if (exo.isPlaying) exo.pause() else exo.play()
                    }
                )
            }
    ) {
        if (isCurrentPage) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exo
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        key(burstKey) {
            if (burstKey > 0) {
                var show by remember { mutableStateOf(true) }
                val scale = remember { Animatable(0.5f) }
                LaunchedEffect(Unit) {
                    scale.animateTo(1.3f, tween(300, easing = FastOutSlowInEasing))
                    kotlinx.coroutines.delay(400)
                    show = false
                }
                if (show) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Favorite, null,
                            tint = HeartRed,
                            modifier = Modifier.size(140.dp).scale(scale.value)
                        )
                    }
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 90.dp, end = 80.dp)
        ) {
            val displayName = when {
                video.uploaderHandle.isNotBlank() -> "@" + video.uploaderHandle
                video.uploaderName.isNotBlank() -> video.uploaderName
                else -> "earny_user"
            }
            Text(displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (video.caption.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(video.caption, color = Color.White, fontSize = 14.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MusicNote, null,
                    tint = Color.White, modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Original sound - earny", color = Color.White, fontSize = 12.sp)
            }
        }

        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Gold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        video.uploaderHandle.firstOrNull()?.uppercase() ?: "E",
                        color = EarnyBlack,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    Modifier.size(20.dp)
                        .background(if (following) Gold else HeartRed, CircleShape)
                        .align(Alignment.BottomCenter).offset(y = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { following = !following },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (following) Icons.Filled.Check else Icons.Filled.Add, null,
                        tint = if (following) EarnyBlack else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            ActionItem(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                count = formatCount(video.likes),
                tint = if (isLiked) HeartRed else Color.White
            ) { onToggleLike() }

            ActionItem(Icons.Filled.ChatBubble, formatCount(video.comments)) {}
            ActionItem(Icons.Filled.Bookmark, "0") {}
            ActionItem(Icons.Filled.Share, "0") {
                Toast.makeText(ctx, "Shared", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon, null, tint = tint,
            modifier = Modifier.size(30.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(count, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
