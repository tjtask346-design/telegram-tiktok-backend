@file:OptIn(ExperimentalFoundationApi::class)

package com.aim.earny.ui.screens.onboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.components.EarnyButton
import com.aim.earny.ui.components.ProgressDots
import com.aim.earny.ui.theme.*
import kotlinx.coroutines.launch

private data class OnboardPage(
    val grad: List<Color>,
    val title: String,
    val subtitle: String,
    val kind: Int
)

private val PAGES = listOf(
    OnboardPage(GradPurplePink, "Your Content,\nYour Earnings", "Create once, earn forever", 1),
    OnboardPage(GradBlueCyan, "Watch 15s,\nEarn Real", "Every second counts", 2),
    OnboardPage(GradGoldOrange, "Login Without\nPassword", "Magic link, no hassle", 3)
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pager) { page ->
            val p = PAGES[page]
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(p.grad))) {
                // Soft radial highlight
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.3f),
                            radius = size.minDimension * 0.7f
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.3f),
                        radius = size.minDimension * 0.7f
                    )
                }

                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Illustration
                    when (p.kind) {
                        1 -> CreateIllustration()
                        2 -> WatchIllustration()
                        3 -> MagicIllustration()
                    }

                    Spacer(Modifier.height(56.dp))

                    Text(
                        p.title,
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 42.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        p.subtitle,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        TextButton(
            onClick = onDone,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp)
        ) {
            Text("Skip", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Column(
            Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProgressDots(total = PAGES.size, current = pager.currentPage)
            Spacer(Modifier.height(28.dp))
            EarnyButton(
                text = if (pager.currentPage == PAGES.size - 1) "Get Started" else "Next",
                onClick = {
                    if (pager.currentPage == PAGES.size - 1) onDone()
                    else scope.launch {
                        pager.animateScrollToPage(pager.currentPage + 1)
                    }
                }
            )
        }
    }
}

@Composable
private fun CreateIllustration() {
    val trans = rememberInfiniteTransition(label = "create")
    val rot by trans.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "r"
    )

    Box(
        Modifier.size(220.dp).clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = size.minDimension / 2 - 10

            // Outer glowing ring
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = r,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 3f)
            )

            // Orbiting dot
            val angle = Math.toRadians(rot.toDouble())
            val dx = cx + (r - 12) * kotlin.math.cos(angle).toFloat()
            val dy = cy + (r - 12) * kotlin.math.sin(angle).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, Color.White.copy(alpha = 0.6f))
                ),
                radius = 14f, center = Offset(dx, dy)
            )
        }

        // Center icon: play + coin
        Box(
            Modifier.size(120.dp).clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                null, tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
        }

        // Coin badge
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(Color(0xFFFFD700))
                .align(Alignment.BottomEnd).offset(x = (-20).dp, y = (-20).dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Star,
                null, tint = Color(0xFFB8860B),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun WatchIllustration() {
    val trans = rememberInfiniteTransition(label = "watch")
    val pulse by trans.animateFloat(
        0.9f, 1.05f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p"
    )

    Box(
        Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = size.minDimension / 2 - 8

            // Clock rings
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = r, center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = r * 0.72f, center = Offset(cx, cy),
                style = Stroke(width = 4f)
            )
            // Tick marks
            for (i in 0 until 12) {
                val a = Math.toRadians((i * 30).toDouble())
                val x1 = cx + (r - 12) * kotlin.math.cos(a).toFloat()
                val y1 = cy + (r - 12) * kotlin.math.sin(a).toFloat()
                val x2 = cx + (r - 4) * kotlin.math.cos(a).toFloat()
                val y2 = cy + (r - 4) * kotlin.math.sin(a).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(x1, y1), end = Offset(x2, y2),
                    strokeWidth = 3f
                )
            }
        }

        // Center: 15s text
        Box(
            Modifier.size(120.dp * pulse).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "15",
                    color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "seconds",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Coin badge
        Box(
            Modifier.size(56.dp).clip(CircleShape)
                .background(Color(0xFFFFD700))
                .align(Alignment.TopEnd).offset(x = (-20).dp, y = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.AttachMoney,
                null, tint = Color(0xFFB8860B),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun MagicIllustration() {
    val trans = rememberInfiniteTransition(label = "magic")
    val float by trans.animateFloat(
        -8f, 8f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "f"
    )
    val sparkle by trans.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s"
    )

    Box(
        Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Envelope
        Box(
            Modifier.size(140.dp, 100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.25f))
                .offset(y = float.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Email, null,
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        // Floating coin (top right)
        Box(
            Modifier.size(56.dp).clip(CircleShape)
                .background(Color(0xFFFFD700))
                .align(Alignment.TopEnd).offset(x = (-20).dp, y = 30.dp)
                .scale(sparkle),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "e",
                color = Color(0xFFB8860B),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Sparkles
        listOf(
            0.15f to 0.3f,
            0.85f to 0.25f,
            0.2f to 0.85f,
            0.8f to 0.75f
        ).forEach { (fx, fy) ->
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .align(Alignment.TopStart)
                    .offset(x = (220 * fx).dp, y = (220 * fy).dp)
                    .scale(sparkle)
            )
        }
    }
}
