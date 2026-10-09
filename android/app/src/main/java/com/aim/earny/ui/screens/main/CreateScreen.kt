package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.theme.*

@Composable
fun CreateScreen() {
    Box(Modifier.fillMaxSize().background(EarnyBlack)) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF1A0F14), Color.Black))
            )
        )
        Text(
            "Create",
            color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
        )

        Box(
            Modifier.align(Alignment.Center).size(90.dp).background(HeartRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(28.dp).background(Color.White, CircleShape)
            )
        }

        Text(
            "Tap to record",
            color = TextWhite60, fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Center).padding(top = 140.dp)
        )
    }
}
