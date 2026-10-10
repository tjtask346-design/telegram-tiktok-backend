@file:OptIn(ExperimentalFoundationApi::class)

package com.aim.earny.ui.screens.main

import android.content.Intent
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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aim.earny.BuildConfig
import com.aim.earny.data.AppEvents
import com.aim.earny.data.Video
import com.aim.earny.data.formatCount
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.FeedViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun FeedScreen(
    onOpenProfile: (String) -> Unit = {},
    vm: FeedViewModel = viewModel()
) {
    val allVideos by vm.videos.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val likedIds by vm.likedIds.collectAsStateWithLifecycle()
    val followingIds by vm.followingIds.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(1) } // 0 = Following, 1 = For You

    val feedRefresh by AppEvents.feedRefresh.collectAsStateWithLifecycle()
    LaunchedEffect(feedRefresh) { vm.load() }

    val myUid = FirebaseAuth.getInstance().currentUser?.uid

    // Local filter for Following tab
    val videos = remember(tab, allVideos, followingIds, myUid) {
        if (tab == 0) {
            allVideos.filter {
                followingIds.contains(it.uploader) || it.uploader == myUid
            }
        } else allVideos
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            loading && allVideos.isEmpty() -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Gold) }

            error != null && allVideos.isEmpty() -> Column(
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
                    if (tab == 0) Icons.Filled.People else Icons.Filled.Videocam, null,
                    tint = Gold.copy(alpha = 0.7f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (tab == 0) "No followed creators yet" else "No videos yet",
                    color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (tab == 0) "Follow creators to see their videos here"
                    else "Tap the Earny Orb to post the first one",
                    color = TextWhite60, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            else -> {
                val pager = rememberPagerState(pageCount = { videos.size })
                val currentIndex = pager.currentPage
                val ctx = LocalContext.current

                val exo = remember {
                    ExoPlayer.Builder(ctx)
                        .build()
                        .apply {
                            repeatMode = ExoPlayer.REPEAT_MODE_ONE
                            playWhenReady = true
                        }
                }

                DisposableEffect(Unit) { onDispose { exo.release() } }

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
                        isFollowing = followingIds.contains(v.uploader),
                        isMine = v.uploader == myUid,
                        onToggleLike = { vm.toggleLike(v) },
                        onToggleFollow = { vm.toggleFollow(v.uploader) },
                        onBecameVisible = { vm.onPageVisible(v) },
                        onOpenProfile = { onOpenProfile(v.uploader) }
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
    isFollowing: Boolean,
    isMine: Boolean,
    onToggleLike: () -> Unit,
    onToggleFollow: () -> Unit,
    onBecameVisible: () -> Unit,
    onOpenProfile: () -> Unit
) {
    var burstKey by remember { mutableStateOf(0) }
    val ctx = LocalContext.current

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
                    Modifier.size(48.dp).clip(CircleShape).background(Gold)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onOpenProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        video.uploaderHandle.firstOrNull()?.uppercase()
                            ?: video.uploaderName.firstOrNull()?.uppercase()
                            ?: "E",
                        color = EarnyBlack,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Hide follow badge on own videos
                if (!isMine) {
                    Box(
                        Modifier.size(20.dp)
                            .background(if (isFollowing) Gold else HeartRed, CircleShape)
                            .align(Alignment.BottomCenter).offset(y = 8.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggleFollow() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isFollowing) Icons.Filled.Check else Icons.Filled.Add, null,
                            tint = if (isFollowing) EarnyBlack else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            ActionItem(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                count = formatCount(video.likes),
                tint = if (isLiked) HeartRed else Color.White
            ) { onToggleLike() }

            ActionItem(Icons.Filled.ChatBubble, formatCount(video.comments)) {}
            ActionItem(Icons.Filled.Bookmark, "0") {}
            ActionItem(Icons.Filled.Share, formatCount(video.likes)) {
                val url = BuildConfig.API_BASE.trimEnd('/') + "/stream/" + video.telegramMsgId
                val text = if (video.caption.isNotBlank())
                    "${video.caption}

$url" else url
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                try {
                    ctx.startActivity(Intent.createChooser(intent, "Share video"))
                } catch (_: Exception) {
                    Toast.makeText(ctx, "No share app", Toast.LENGTH_SHORT).show()
                }
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
