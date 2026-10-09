package com.aim.earny.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
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
import com.aim.earny.vm.UsernameState

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onGoLogin: () -> Unit = {},
    vm: AuthViewModel = viewModel()
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val unameState by vm.usernameState.collectAsState()

    val emailValid = android.util.Patterns.EMAIL_ADDRESS
        .matcher(email.trim()).matches()

    val canSubmit = firstName.isNotBlank()
        && lastName.isNotBlank()
        && emailValid
        && password.length >= 6
        && unameState == UsernameState.Available
        && !loading

    Column(
        Modifier.fillMaxSize().background(EarnyBlack)
            .verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        EarnyOrb(size = 68.dp)

        Spacer(Modifier.height(20.dp))

        Text("Create your account", color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Claim your @username — it's your identity",
            color = TextWhite60, fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Name row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                EarnyTextField(
                    value = firstName,
                    onValueChange = { if (it.length <= 30) { firstName = it; vm.clearError() } },
                    placeholder = "First name"
                )
            }
            Box(Modifier.weight(1f)) {
                EarnyTextField(
                    value = lastName,
                    onValueChange = { if (it.length <= 30) { lastName = it; vm.clearError() } },
                    placeholder = "Last name"
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Username with live check
        EarnyTextField(
            value = username,
            onValueChange = {
                val v = it.lowercase().filter { ch ->
                    ch.isLetterOrDigit() || ch == '_' || ch == '.'
                }
                if (v.length <= 20) {
                    username = v
                    vm.checkUsername(v)
                    vm.clearError()
                }
            },
            placeholder = "@username",
            isError = unameState == UsernameState.Taken || unameState == UsernameState.Invalid,
            trailingIcon = {
                when (unameState) {
                    UsernameState.Checking -> CircularProgressIndicator(
                        color = Gold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    UsernameState.Available -> Icon(
                        Icons.Filled.Check, null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(22.dp)
                    )
                    else -> {}
                }
            }
        )

        // Username status line
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (txt, col) = when (unameState) {
                UsernameState.Idle -> "3+ chars, a-z 0-9 _ ." to TextWhite40
                UsernameState.Checking -> "Checking availability…" to TextWhite60
                UsernameState.Available -> "✓ @$username is available" to Color(0xFF4CAF50)
                UsernameState.Taken -> "✗ @$username is already taken" to Color(0xFFE53935)
                UsernameState.Invalid -> "Only 3-20 chars: a-z 0-9 _ ." to Color(0xFFE53935)
            }
            Text(txt, color = col, fontSize = 11.sp)
        }

        Spacer(Modifier.height(14.dp))

        EarnyTextField(
            value = email,
            onValueChange = { email = it; vm.clearError() },
            placeholder = "you@example.com",
            leadingIcon = Icons.Filled.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(Modifier.height(12.dp))

        EarnyTextField(
            value = password,
            onValueChange = { password = it; vm.clearError() },
            placeholder = "Password (min 6 chars)",
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

        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(error!!, color = Color(0xFFE53935), fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))

        EarnyButton(
            text = "Create Account",
            enabled = canSubmit,
            loading = loading,
            gradient = true,
            onClick = {
                vm.signUp(firstName, lastName, username, email, password, onSignupSuccess)
            }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "We'll send a verification link to your email",
            color = TextWhite40, fontSize = 12.sp, textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", color = TextWhite60, fontSize = 14.sp)
            Text(
                "Log In",
                color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onGoLogin
                )
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}
