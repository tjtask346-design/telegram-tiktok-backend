package com.aim.earny.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.components.EarnyButton
import com.aim.earny.ui.components.EarnyOrb
import com.aim.earny.ui.theme.*

@Composable
fun AuthChoiceScreen(onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize().background(EarnyBlack)) {

        // Soft gold glow bottom
        Box(
            Modifier.fillMaxWidth().height(300.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Gold.copy(alpha = 0.08f), Color.Transparent),
                        radius = 800f
                    )
                )
        )

        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EarnyOrb(size = 88.dp)
            Spacer(Modifier.height(36.dp))

            Text(
                "Welcome to Earny",
                color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Watch. Create. Earn — just with\nyour email",
                color = TextWhite60, fontSize = 15.sp,
                textAlign = TextAlign.Center, lineHeight = 22.sp
            )

            Spacer(Modifier.height(56.dp))

            EarnyButton(text = "Continue with Email", onClick = onContinue)

            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f).height(1.dp).background(EarnyBorder))
                Text(
                    "  Only email needed  ",
                    color = TextWhite40, fontSize = 11.sp
                )
                Box(Modifier.weight(1f).height(1.dp).background(EarnyBorder))
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Magic link + Password • Secure & simple",
                color = TextWhite40, fontSize = 12.sp, textAlign = TextAlign.Center
            )
        }
    }
}
