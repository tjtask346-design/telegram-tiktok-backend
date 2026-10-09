package com.aim.earny.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.theme.Gold
import com.aim.earny.ui.theme.Orange

@Composable
fun SafeAvatar(name: String, size: Dp = 96.dp, modifier: Modifier = Modifier) {
    val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier.size(size)
            .background(Brush.sweepGradient(listOf(Gold, Orange, Gold)), CircleShape)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            Text(letter, color = Gold, fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
