package com.aim.earny.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aim.earny.ui.components.EarnyButton
import com.aim.earny.ui.components.EarnyTextField
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.AuthViewModel

@Composable
fun ProfileSetupScreen(onDone: () -> Unit, vm: AuthViewModel = viewModel()) {
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    val loading by vm.loading.collectAsState()
    val available = username.length >= 3

    Column(
        Modifier.fillMaxSize().background(EarnyBlack).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // Avatar
        Box(
            Modifier.size(120.dp)
                .background(Brush.sweepGradient(listOf(Gold, Orange, Gold)), CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.fillMaxSize().background(EarnyInput, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (username.isBlank()) "?" else username.first().uppercase(),
                    color = Gold, fontSize = 44.sp, fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Filled.CameraAlt, null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp).align(Alignment.BottomEnd)
                        .background(Gold, CircleShape).padding(4.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("Set up your profile", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))

        EarnyTextField(
            value = username,
            onValueChange = { if (it.length <= 20) username = it },
            placeholder = "@username",
            trailingIcon = {
                if (username.length >= 3) {
                    Text(
                        if (available) "✓" else "✗",
                        color = if (available) Color(0xFF4CAF50) else Color(0xFFE53935),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        EarnyTextField(
            value = bio,
            onValueChange = { if (it.length <= 80) bio = it },
            placeholder = "Tell us about yourself (max 80)",
            singleLine = false
        )

        Spacer(Modifier.height(32.dp))

        EarnyButton(
            text = "Start Earning",
            enabled = username.length >= 3,
            loading = loading,
            gradient = true,
            onClick = { vm.updateProfile(username, bio, onDone) }
        )
    }
}
