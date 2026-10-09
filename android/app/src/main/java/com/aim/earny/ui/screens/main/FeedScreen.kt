@file:OptIn(ExperimentalFoundationApi::class)

package com.aim.earny.ui.screens.main

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.theme.*

private data class DemoVideo(
    val user: String,
    val caption: String,
    val grad: List<Color>,
    val likes: String,
    val comments: String,
    val bookmarks: String,
    val shares: String
)

private val DEMO = listOf(
    DemoVideo("@earny.creator", "This trend pays 💸 #earny #fyp", GradPurplePink, "124.5K", "2.3K", "14.2K", "8.9K"),
    DemoVideo("@skyline", "Sunset vibes 🌅 #earny", GradBlueCyan, "89.1K", "1.1K", "5.4K", "3.2K"),
    DemoVideo("@arif.codes", "Morning routine ✨", GradOrangeRed, "45.7K", "890", "2.1K", "1.5K"),
    DemoVideo("@mira.daily", "Chill beats 🎧 #lofi", GradGreenTeal, "210.4K", "3.4K", "22.8K", "11.6K")
)

@Composable
fun FeedScreen(onSignOut: () -> Unit) {
    var tab by remember { mutableStateOf(1) }
    val pager = rememberPagerState(pageCount = { DEMO.size })

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            VideoPage(DEMO[page], page)
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
private fun VideoPage(video: DemoVideo, pageIndex: Int) {
    var liked by remember { mutableStateOf(false) }
    var following by remember { mutableStateOf(false) }
    var burstKey by remember { mutableStateOf(0) }
    val infinite = rememberInfiniteTransition(label = "tick")

    val ticker by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "tick"
    )

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(video.grad))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!liked) liked = true
                        burstKey++
                    }
                )
            }
    ) {
        // Big heart burst
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

        // Bottom info
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 90.dp, end = 80.dp)
        ) {
            Text(video.user, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(video.caption, color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Original sound - earny.creator",
                    color = Color.White, fontSize = 12.sp,
                    modifier = Modifier.offset(x = (-40 * (1f - ticker)).dp)
                )
            }
        }

        // Right actions
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Avatar
            Box {
                Box(
                    Modifier.size(48.dp).background(Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(video.user.drop(1).take(1).uppercase(), color = EarnyBlack, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.size(20.dp).background(if (following) Gold else HeartRed, CircleShape)
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
                count = video.likes,
                tint = if (liked) HeartRed else Color.White,
                iconScale = if (liked) 1.15f else 1f
            ) { if (!liked) liked = true }

            ActionItem(Icons.Filled.ChatBubble, video.comments) {}
            ActionItem(Icons.Filled.Bookmark, video.bookmarks) {}
            ActionItem(Icons.Filled.Share, video.shares) {}
        }

        // Progress bar at bottom
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                .fillMaxWidth(0.4f).height(2.dp)
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(ticker).background(Gold))
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    tint: Color = Color.White,
    iconScale: Float = 1f,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon, null,
            tint = tint,
            modifier = Modifier.size(30.dp).scale(iconScale).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(count, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
