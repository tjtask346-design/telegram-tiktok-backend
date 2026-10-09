package com.aim.earny.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aim.earny.ui.theme.Gold
import com.aim.earny.ui.theme.TextWhite40

@Composable
fun ProgressDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val active = i == current
            val w by animateDpAsState(if (active) 24.dp else 8.dp, label = "w")
            val c by animateColorAsState(if (active) Gold else TextWhite40, label = "c")
            Box(
                Modifier.width(w).height(8.dp)
                    .background(c, RoundedCornerShape(100.dp))
            )
        }
    }
}
