package com.aim.earny.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.aim.earny.ui.theme.EarnyBlack
import com.aim.earny.ui.theme.Gold
import com.aim.earny.ui.theme.Orange

@Composable
fun EarnyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    gradient: Boolean = false
) {
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn"
    )

    val bg: Brush = if (gradient) {
        Brush.horizontalGradient(listOf(Gold, Orange))
    } else {
        Brush.horizontalGradient(listOf(Color.White, Color.White))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(pressScale)
            .background(
                brush = if (enabled) bg else Brush.horizontalGradient(
                    listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))
                ),
                shape = RoundedCornerShape(100.dp)
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(color = EarnyBlack, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Text(
                text,
                color = if (enabled) EarnyBlack else Color(0xFF666666),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
