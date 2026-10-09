package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.theme.*

private val GRADS = listOf(GradPurplePink, GradBlueCyan, GradOrangeRed, GradGreenTeal, GradGoldOrange, GradBlueCyan)

@Composable
fun ProfileScreen(onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().background(EarnyBlack).padding(top = 48.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(72.dp).background(Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("E", color = EarnyBlack, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("@earny.creator", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Creating & earning 💰", color = TextWhite60, fontSize = 13.sp)
            }
            Icon(
                Icons.Filled.Logout, "logout",
                tint = TextWhite60,
                modifier = Modifier.size(24.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onSignOut
                )
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Stat("1.2M", "Followers", Modifier.weight(1f))
            Stat("340", "Following", Modifier.weight(1f))
            Stat("28.4K", "Likes", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Bio • Earny ambassador • DM for collabs",
            color = TextWhite60, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items((0 until 12).toList()) { i ->
                Box(
                    Modifier.fillMaxWidth().aspectRatio(0.75f)
                        .background(Brush.verticalGradient(GRADS[i % GRADS.size]), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextWhite60, fontSize = 12.sp)
    }
}
