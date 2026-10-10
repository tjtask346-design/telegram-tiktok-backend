@file:OptIn(ExperimentalFoundationApi::class)

package com.aim.earny.ui.screens.main

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aim.earny.BuildConfig
import com.aim.earny.data.Video
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.FeedViewModel

@Composable
fun FeedScreen(vm: FeedViewModel = viewModel()) {
    val videos by vm.videos.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) { vm.load() }

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
                VerticalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                    VideoPage(videos[page])
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
private fun VideoPage(video: Video) {
    var liked by remember { mutableStateOf(false) }
    var following by remember { mutableStateOf(false) }
    var burstKey by remember { mutableStateOf(0) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    // ---- ExoPlayer setup with error listener ----
    val exo = remember {
        ExoPlayer.Builder(ctx).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isPlaying = state == Player.STATE_READY
                    if (state == Player.STATE_ENDED) playbackError = null
                }
                override fun onPlayerError(err: PlaybackException) {
                    playbackError = err.errorCodeName + ": " + (err.message ?: "unknown")
                }
            })
        }
    }

    val streamUrl = remember(video.telegramMsgId) {
        if (video.telegramMsgId > 0)
            BuildConfig.API_BASE.trimEnd('/') + "/stream/" + video.telegramMsgId
        else null
    }

    LaunchedEffect(video.id) {
        playbackError = null
        if (streamUrl != null) {
            exo.setMediaItem(MediaItem.fromUri(streamUrl))
            exo.prepare()
            exo.play()
        }
    }

    DisposableEffect(Unit) { onDispose { exo.release() } }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!liked) liked = true
                        burstKey++
                    },
                    onTap = {
                        if (exo.isPlaying) exo.pause() else exo.play()
                    }
                )
            }
    ) {
        // ---- Video surface OR fallback ----
        if (streamUrl != null) {
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
        } else {
            // No telegram id → gradient placeholder
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(GradPurplePink[0], GradPurplePink[1])
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Video pending…",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp
                )
            }
        }

        // ---- Error overlay ----
        if (playbackError != null) {
            Box(
                Modifier.align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.ErrorOutline, null,
                        tint = HeartRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Playback failed",
                        color = Color.White, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        playbackError!!,
                        color = TextWhite60, fontSize = 10.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.background(Gold, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                playbackError = null
                                exo.prepare()
                                exo.play()
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Retry",
                            color = EarnyBlack,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ---- Heart burst ----
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

        // ---- Bottom info ----
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

        // ---- Right side actions ----
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
                icon = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                count = (video.likes + if (liked) 1 else 0).toString(),
                tint = if (liked) HeartRed else Color.White
            ) { if (!liked) liked = true }

            ActionItem(Icons.Filled.ChatBubble, video.comments.toString()) {}
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
