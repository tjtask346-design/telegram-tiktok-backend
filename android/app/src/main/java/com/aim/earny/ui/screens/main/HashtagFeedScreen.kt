package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.data.HashtagRepository
import com.aim.earny.data.Video
import com.aim.earny.data.formatCount
import com.aim.earny.ui.theme.*

@Composable
fun HashtagFeedScreen(
    tag: String,
    onBack: () -> Unit,
    onOpenVideo: (Video) -> Unit
) {
    val repo = remember { HashtagRepository() }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(tag) {
        loading = true
        videos = repo.videosForTag(tag)
        loading = false
    }

    Column(Modifier.fillMaxSize().background(EarnyBlack).padding(top = 40.dp)) {

        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ArrowBack, "back",
                tint = TextWhite,
                modifier = Modifier.size(24.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onBack
                )
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "#${tag.lowercase()}",
                    color = Gold, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "${videos.size} videos",
                    color = TextWhite60, fontSize = 12.sp
                )
            }
        }

        when {
            loading -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Gold) }

            videos.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "#${tag.lowercase()}",
                    color = Gold, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "No videos yet with this hashtag",
                    color = TextWhite60, fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videos) { v ->
                    HashtagTile(v, onClick = { onOpenVideo(v) })
                }
            }
        }
    }
}

@Composable
private fun HashtagTile(video: Video, onClick: () -> Unit) {
    val gradients = listOf(
        GradPurplePink, GradBlueCyan, GradOrangeRed,
        GradGreenTeal, GradGoldOrange
    )
    val idx = video.id.hashCode().let { if (it < 0) -it else it } % gradients.size
    val grad = gradients[idx]

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(grad))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
    ) {
        if (video.thumbB64.isNotBlank()) {
            com.aim.earny.ui.components.VideoThumbBase64(
                b64 = video.thumbB64,
                modifier = Modifier.fillMaxSize(),
                showPlayIcon = false
            )
        }
        Row(
            Modifier.align(Alignment.BottomStart).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Filled.PlayArrow, null,
                tint = Color.White, modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                formatCount(video.views),
                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}
