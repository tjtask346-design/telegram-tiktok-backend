package com.aim.earny.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.aim.earny.BuildConfig
import com.aim.earny.ui.theme.Gold
import com.aim.earny.ui.theme.Orange

@Composable
fun SafeAvatar(
    name: String,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier,
    showStoryRing: Boolean = false,
    picMsgId: Long = 0L
) {
    val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    val outerMod = if (showStoryRing) {
        modifier
            .size(size)
            .background(Brush.sweepGradient(listOf(Gold, Orange, Gold)), CircleShape)
            .padding(3.dp)
    } else {
        modifier.size(size)
    }

    Box(
        modifier = outerMod,
        contentAlignment = Alignment.Center
    ) {
        if (picMsgId > 0L) {
            SubcomposeAsyncImage(
                model = BuildConfig.API_BASE.trimEnd('/') + "/profile-pic/" + picMsgId,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    is AsyncImagePainter.State.Loading -> {
                        Box(
                            Modifier.fillMaxSize().clip(CircleShape)
                                .background(Color(0xFF1E1E1E)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Gold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(size * 0.3f)
                            )
                        }
                    }
                    else -> LetterFallback(letter, size)
                }
            }
        } else {
            LetterFallback(letter, size)
        }
    }
}

@Composable
private fun LetterFallback(letter: String, size: Dp) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Gold,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
