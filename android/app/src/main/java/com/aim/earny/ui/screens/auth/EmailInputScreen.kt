package com.aim.earny.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.components.EarnyButton
import com.aim.earny.ui.components.EarnyTextField
import com.aim.earny.ui.components.ProgressDots
import com.aim.earny.ui.theme.*

@Composable
fun EmailInputScreen(
    onBack: () -> Unit,
    onMagicLink: (String) -> Unit,
    onPassword: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("magic") } // "magic" | "password" | "signup"
    val valid = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    Column(Modifier.fillMaxSize().background(EarnyBlack)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ArrowBack, "back",
                tint = TextWhite,
                modifier = Modifier.size(28.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onBack
                )
            )
            Spacer(Modifier.weight(1f))
            ProgressDots(total = 3, current = 0)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(28.dp))
        }

        Column(Modifier.padding(horizontal = 28.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("Enter your email", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(28.dp))

            EarnyTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "you@example.com",
                leadingIcon = Icons.Filled.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(Modifier.height(20.dp))

            // Segmented control
            Row(
                Modifier.fillMaxWidth().height(44.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(EarnyInput)
                    .padding(4.dp)
            ) {
                SegTab("Magic Link", mode == "magic", Modifier.weight(1f)) { mode = "magic" }
                SegTab("Password", mode == "password", Modifier.weight(1f)) { mode = "password" }
            }

            Spacer(Modifier.height(32.dp))

            EarnyButton(
                text = "Continue",
                enabled = valid,
                onClick = {
                    if (mode == "magic") onMagicLink(email.trim())
                    else onPassword(email.trim())
                }
            )
        }
    }
}

@Composable
private fun SegTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.fillMaxHeight()
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) EarnyBlack else TextWhite60,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
