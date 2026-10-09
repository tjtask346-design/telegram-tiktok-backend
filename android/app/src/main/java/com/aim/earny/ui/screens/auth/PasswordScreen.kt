package com.aim.earny.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aim.earny.ui.components.EarnyButton
import com.aim.earny.ui.components.EarnyTextField
import com.aim.earny.ui.components.ProgressDots
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.AuthViewModel

@Composable
fun PasswordScreen(
    email: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    var password by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var isSignup by remember { mutableStateOf(false) }
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    val strength = when {
        password.length >= 10 && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> 3
        password.length >= 8 -> 2
        password.length >= 6 -> 1
        else -> 0
    }

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
            ProgressDots(total = 3, current = 1)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(28.dp))
        }

        Column(Modifier.padding(horizontal = 28.dp)) {
            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.background(EarnyInput, RoundedCornerShape(100.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(email, color = TextWhite60, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                if (isSignup) "Create password" else "Enter password",
                color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))

            EarnyTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password (6+ chars)",
                leadingIcon = Icons.Filled.Lock,
                visualTransformation = if (showPw) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Icon(
                        if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        null, tint = TextWhite40, modifier = Modifier.size(22.dp).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showPw = !showPw }
                    )
                }
            )

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Color(0xFFE53935), Color(0xFFFFC107), Color(0xFF4CAF50)
                ).forEachIndexed { i, c ->
                    Box(
                        Modifier.weight(1f).height(4.dp).background(
                            if (i < strength) c else EarnyBorder,
                            RoundedCornerShape(100.dp)
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (!isSignup) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "Forgot password?",
                        color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { vm.resetPassword(email) }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (error != null) {
                Text(error!!, color = Color(0xFFE53935), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
            }

            EarnyButton(
                text = if (isSignup) "Sign Up" else "Login",
                enabled = password.length >= 6,
                loading = loading,
                onClick = {
                    if (isSignup) vm.signUpWithPassword(email, password, onSuccess)
                    else vm.signInWithPassword(email, password, onSuccess)
                }
            )

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    if (isSignup) "Have an account? " else "New here? ",
                    color = TextWhite60, fontSize = 13.sp
                )
                Text(
                    if (isSignup) "Login" else "Create account",
                    color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isSignup = !isSignup }
                )
            }
        }
    }
}
