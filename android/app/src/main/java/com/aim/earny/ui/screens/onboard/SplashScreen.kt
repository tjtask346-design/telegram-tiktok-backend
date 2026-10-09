package com.aim.earny.ui.screens.onboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.components.EarnyOrb
import com.aim.earny.ui.theme.EarnyBlack
import com.aim.earny.ui.theme.TextWhite60
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onDone()
    }

    Box(
        Modifier.fillMaxSize().background(EarnyBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            EarnyOrb(size = 96.dp, glowing = true)
            Spacer(Modifier.height(32.dp))
            Text(
                "earny",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text("Your Time Pays", color = TextWhite60, fontSize = 12.sp)
        }
    }
}
