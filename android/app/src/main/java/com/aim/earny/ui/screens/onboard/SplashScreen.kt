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
        Modifier.fillMaxSize().background(EarnyBlack)
    ) {
        // পুরো স্ক্রিন জুড়ে ছবি (ব্যাকগ্রাউন্ডসহ)
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.aim.earny.R.drawable.earny_logo),
            contentDescription = "Earny Logo",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // নিচের দিকে কালো গ্রেডিয়েন্ট, যাতে লেখা সুন্দরভাবে ফুটে ওঠে
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, EarnyBlack)
                    )
                )
        )

        // নিচে "earny" লেখাটি
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
