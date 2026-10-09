package com.aim.earny.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aim.earny.R
import com.aim.earny.ui.theme.EarnyInput
import com.aim.earny.ui.theme.Gold
import com.aim.earny.ui.theme.Orange

@Composable
fun EarnyOrb(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    glowing: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val trans = rememberInfiniteTransition(label = "orb")

    val rotation by trans.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    val pulse by trans.animateFloat(
        initialValue = 1f,
        targetValue = if (glowing) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press"
    )

    Box(
        modifier = modifier
            .size(size * pulse)
            .scale(pressScale)
            .then(
                if (onClick != null)
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                tryAwaitRelease()
                                pressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val center = this.center

            rotate(rotation) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(Gold, Orange, Gold, Orange, Gold),
                        center = center
                    ),
                    radius = radius,
                    center = center
                )
            }

            drawCircle(
                color = Color.Black,
                radius = radius - 3.dp.toPx(),
                center = center
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EarnyInput, EarnyInput),
                    center = center,
                    radius = radius - 5.dp.toPx()
                ),
                radius = radius - 5.dp.toPx(),
                center = center
            )
        }

        // Real icon from launcher — instead of "e"
        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = null,
            modifier = Modifier
                .size(size * 0.62f)
                .padding(2.dp)
        )
    }
}
