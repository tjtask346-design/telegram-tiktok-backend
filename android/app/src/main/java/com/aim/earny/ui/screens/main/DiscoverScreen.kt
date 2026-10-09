package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.theme.*

private val GRADS = listOf(GradPurplePink, GradBlueCyan, GradOrangeRed, GradGreenTeal, GradGoldOrange, GradPurplePink)

@Composable
fun DiscoverScreen() {
    Column(Modifier.fillMaxSize().background(EarnyBlack).padding(top = 48.dp)) {
        Text(
            "Discover",
            color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items((0 until 20).toList()) { i ->
                Box(
                    Modifier.fillMaxWidth().height(if (i % 3 == 0) 240.dp else 180.dp)
                        .background(Brush.verticalGradient(GRADS[i % GRADS.size]), RoundedCornerShape(12.dp))
                )
            }
        }
    }
}
