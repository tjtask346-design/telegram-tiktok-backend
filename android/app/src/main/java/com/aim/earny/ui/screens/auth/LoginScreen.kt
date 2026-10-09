package com.aim.earny.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aim.earny.ui.components.EarnyButton
import com.aim.earny.ui.components.EarnyOrb
import com.aim.earny.ui.components.EarnyTextField
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoSignup: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val info by vm.info.collectAsState()

    val canSubmit = email.isNotBlank() && password.length >= 6 && !loading

    Column(
        Modifier.fillMaxSize().background(EarnyBlack)
            .verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        EarnyOrb(size = 72.dp)
        Spacer(Modifier.height(24.dp))

        Text("Welcome back", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Log in to continue earning", color = TextWhite60, fontSize = 14.sp)

        Spacer(Modifier.height(36.dp))

        EarnyTextField(
            value = email, onValueChange = { email = it; vm.clearError() },
            placeholder = "you@example.com",
            leadingIcon = Icons.Filled.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(12.dp))

        EarnyTextField(
            value = password, onValueChange = { password = it; vm.clearError() },
            placeholder = "Password",
            leadingIcon = Icons.Filled.Lock,
            visualTransformation = if (showPw) VisualTransformation.None
                else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Icon(
                    if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    null, tint = TextWhite40,
                    modifier = Modifier.size(22.dp).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showPw = !showPw }
                )
            }
        )

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "Forgot password?",
                color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (email.isNotBlank()) vm.resetPassword(email)
                }
            )
        }

        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(error!!, color = Color(0xFFE53935), fontSize = 13.sp,
                textAlign = TextAlign.Center)
        }
        if (info != null) {
            Spacer(Modifier.height(16.dp))
            Text(info!!, color = Color(0xFF4CAF50), fontSize = 13.sp,
                textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))

        EarnyButton(
            text = "Log In",
            enabled = canSubmit,
            loading = loading,
            gradient = true,
            onClick = { vm.signIn(email, password, onLoginSuccess) }
        )

        Spacer(Modifier.height(20.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("New here? ", color = TextWhite60, fontSize = 14.sp)
            Text(
                "Create account",
                color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onGoSignup
                )
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}
