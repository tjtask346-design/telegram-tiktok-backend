package com.aim.earny.ui.screens.auth

import android.content.Intent
import android.net.Uri
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aim.earny.ui.components.EarnyButton
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun VerifyEmailScreen(
    email: String,
    onVerified: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    val ctx = LocalContext.current
    var seconds by remember { mutableStateOf(45) }
    var checking by remember { mutableStateOf(false) }
    var resending by remember { mutableStateOf(false) }

    // Auto-poll for verification every 3 seconds
    LaunchedEffect(email) {
        while (true) {
            delay(3000)
            if (vm.checkVerified()) {
                onVerified()
                break
            }
        }
    }

    // Resend countdown
    LaunchedEffect(seconds) {
        if (seconds > 0) {
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
            Modifier.size(120.dp)
                .background(Gold.copy(alpha = 0.1f), RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Email, null, tint = Gold, modifier = Modifier.size(60.dp))
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Verify your email",
            color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(14.dp))

        Text(
            "We sent a verification link to:",
            color = TextWhite60, fontSize = 14.sp, textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            email,
            color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Open the email, tap the link,\nand this screen will unlock automatically ✨",
            color = TextWhite60, fontSize = 13.sp,
            textAlign = TextAlign.Center, lineHeight = 19.sp
        )

        Spacer(Modifier.height(40.dp))

        EarnyButton(
            text = "Open Gmail",
            onClick = {
                val gmail = ctx.packageManager.getLaunchIntentForPackage("com.google.android.gm")
                if (gmail != null) {
                    gmail.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    ctx.startActivity(gmail)
                } else {
                    val web = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://mail.google.com"))
                    ctx.startActivity(web)
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        if (seconds > 0) {
            Text(
                "Resend in ${seconds}s",
                color = TextWhite40, fontSize = 14.sp
            )
        } else {
            TextButton(
                onClick = {
                    if (resending) return@TextButton
                    resending = true
                    vm.resend()
                    seconds = 45
                    resending = false
                }
            ) {
                Text("Resend Link", color = Gold, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = Gold, strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp)
            )
            Text(
                if (checking) "Checking..." else "Waiting for verification…",
                color = TextWhite40, fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Didn't get the email? Check your spam folder",
            color = TextWhite40, fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
