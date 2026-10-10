package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aim.earny.BuildConfig
import com.aim.earny.data.Video
import com.aim.earny.ui.theme.EarnyBlack
import com.aim.earny.ui.theme.TextWhite
import com.aim.earny.ui.theme.TextWhite60

@Composable
fun VideoPlayerOverlay(
    video: Video,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current

    val exo = remember {
        ExoPlayer.Builder(ctx).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    LaunchedEffect(video.id) {
        if (video.telegramMsgId > 0) {
            val url = BuildConfig.API_BASE.trimEnd('/') + "/stream/" + video.telegramMsgId
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.play()
        }
    }

    DisposableEffect(Unit) { onDispose { exo.release() } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exo
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Back button top-left
        Row(
            Modifier.align(Alignment.TopStart)
                .padding(12.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onClose
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ArrowBack, "back",
                tint = Color.White, modifier = Modifier.size(26.dp)
            )
        }

        // Caption bottom
        if (video.caption.isNotBlank()) {
            Column(
                Modifier.align(Alignment.BottomStart)
                    .padding(20.dp)
                    .padding(bottom = 40.dp)
            ) {
                val name = if (video.uploaderHandle.isNotBlank())
                    "@${video.uploaderHandle}" else video.uploaderName
                Text(
                    name, color = TextWhite,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(video.caption, color = TextWhite60, fontSize = 13.sp)
            }
        }
    }
}
