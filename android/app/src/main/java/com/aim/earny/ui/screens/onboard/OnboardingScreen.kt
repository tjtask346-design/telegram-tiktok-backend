package com.aim.earny.ui.screens.onboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val subtitle: String
)

private val PAGES = listOf(
    OnboardPage(GradPurplePink, "Your Content,\nYour Earnings", "Create once, earn forever"),
    OnboardPage(GradBlueCyan, "Watch 15s,\nEarn Real", "Every second counts"),
    OnboardPage(GradGoldOrange, "Login Without\nPassword", "Magic link, no hassle")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pager) { page ->
            val p = PAGES[page]
            Box(
                Modifier.fillMaxSize().background(Brush.verticalGradient(p.grad)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
        ) { Text("Skip", color = Color.White, fontWeight = FontWeight.SemiBold) }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProgressDots(total = PAGES.size, current = pager.currentPage)
            Spacer(Modifier.height(28.dp))
            EarnyButton(
                text = if (pager.currentPage == PAGES.size - 1) "Get Started" else "Next",
                onClick = {
                    if (pager.currentPage == PAGES.size - 1) onDone()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
            )
        }
    }
}
