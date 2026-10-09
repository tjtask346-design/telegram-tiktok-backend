package com.aim.earny.ui.screens.auth

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.components.EarnyButton
import com.aim.earny.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MagicLinkSentScreen(
    email: String,
    onResend: () -> Unit,
    onUsePassword: () -> Unit
) {
    val ctx = LocalContext.current
    var seconds by remember { mutableStateOf(45) }

    LaunchedEffect(email) {
        seconds = 45
        while (seconds > 0) {
            delay(1000)
            seconds -= 1
        }
    }

    Column(
        Modifier.fillMaxSize().background(EarnyBlack).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(120.dp).background(Gold.copy(alpha = 0.1f), RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Email, null, tint = Gold, modifier = Modifier.size(60.dp))
        }

        Spacer(Modifier.height(32.dp))
        Text("Link sent, darling!", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "We sent a magic link to\n$email\nTap to login instantly ✨",
            color = TextWhite60, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
        )

        Spacer(Modifier.height(40.dp))

        EarnyButton(
            text = "Open Gmail",
            onClick = {
                val i = ctx.packageManager.getLaunchIntentForPackage("com.google.android.gm")
                if (i != null) {
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    ctx.startActivity(i)
                } else {
                    val web = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://mail.google.com"))
                    ctx.startActivity(web)
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        if (seconds > 0) {
            Text("Resend in ${seconds}s", color = TextWhite40, fontSize = 14.sp)
        } else {
            TextButton(onClick = { onResend(); seconds = 45 }) {
                Text("Resend Link", color = Gold, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Login with password instead",
            color = TextWhite60, fontSize = 13.sp,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onUsePassword
            )
        )

        Spacer(Modifier.height(32.dp))

        Text("Didn't get? Check spam folder", color = TextWhite40, fontSize = 12.sp)
    }
}
