package com.aim.earny.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.aim.earny.BuildConfig
import com.aim.earny.data.Video
import com.aim.earny.vm.FeedViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(onSignOut: () -> Unit, vm: FeedViewModel = viewModel()) {
    val videos by vm.videos.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            loading && videos.isEmpty() -> CircularProgressIndicator(
                color = Color(0xFF00E5A0),
                modifier = Modifier.align(Alignment.Center)
            )
            error != null && videos.isEmpty() -> Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error: $error", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.load() }) { Text("Retry") }
            }
            videos.isEmpty() -> Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No videos yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Go to Upload tab to add the first one", color = Color.Gray, fontSize = 13.sp)
            }
            else -> {
                val pager = rememberPagerState(pageCount = { videos.size })
                VerticalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                    val v = videos[page]
                    VideoPage(video = v, onViewed = { vm.markViewed(v.id) }, onLike = { vm.like(v.id) })
                }
            }
        }

        IconButton(
            onClick = onSignOut,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        ) {
            Icon(Icons.Filled.Logout, "Sign out", tint = Color.White)
        }
    }
}

@Composable
private fun VideoPage(video: Video, onViewed: () -> Unit, onLike: () -> Unit) {
    var liked by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    val exo = remember {
        ExoPlayer.Builder(ctx).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    LaunchedEffect(video.id) {
        val url = BuildConfig.API_BASE.trimEnd('/') + "/stream/" + video.telegramMsgId
        exo.setMediaItem(MediaItem.fromUri(url))
        exo.prepare()
        onViewed()
    }

    DisposableEffect(Unit) { onDispose { exo.release() } }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (exo.isPlaying) exo.pause() else exo.play() },
                    onDoubleTap = { if (!liked) { liked = true; onLike() } }
                )
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exo
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (video.caption.isNotBlank()) {
            Text(
                video.caption,
                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 80.dp, bottom = 30.dp)
            )
        }

        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = { if (!liked) { liked = true; onLike() } }) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    "Like",
                    tint = if (liked) Color(0xFFFF3B5C) else Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                (video.likes + if (liked) 1 else 0).toString(),
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}
