package com.aim.earny.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.data.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF00E5A0),
    unfocusedBorderColor = Color.DarkGray,
    focusedLabelColor = Color(0xFF00E5A0),
    unfocusedLabelColor = Color.Gray,
)

/* ═══════════════ LOGIN ═══════════════ */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onGoSignup: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = remember { AuthRepository() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSubmit = !loading && email.isNotBlank() && password.isNotBlank()

    Column(
        Modifier.fillMaxSize().background(Color(0xFF0A0E14)).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Earny", color = Color(0xFF00E5A0), fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text("Sign in to continue", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(36.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it; error = null },
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(), colors = authFieldColors()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it; error = null },
            label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(), colors = authFieldColors()
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = Color(0xFFFF5C5C), fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            enabled = canSubmit,
            onClick = {
                scope.launch {
                    loading = true; error = null
                    try {
                        repo.signIn(email, password)
                        onLoggedIn()
                    } catch (e: Exception) {
                        error = repo.friendlyError(e)
                    } finally { loading = false }
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5A0))
        ) {
            if (loading) CircularProgressIndicator(
                color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp
            ) else Text("Sign In", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account? ", color = Color.Gray, fontSize = 14.sp)
            Text(
                "Sign Up",
                color = Color(0xFF00E5A0), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onGoSignup
                )
            )
        }
    }
}

/* ═══════════════ SIGNUP ═══════════════ */
@Composable
fun SignupScreen(onSignedUp: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = remember { AuthRepository() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val emailOk = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passOk = password.length >= 6
    val matchOk = password == confirm && confirm.isNotEmpty()
    val canSubmit = !loading && emailOk && passOk && matchOk

    Column(
        Modifier.fillMaxSize().background(Color(0xFF0A0E14)).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create Account", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "A magic link will be sent to your email\nto verify your account.",
            color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it; error = null },
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(), colors = authFieldColors(),
            isError = email.isNotEmpty() && !emailOk
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it; error = null },
            label = { Text("Password (min 6 chars)") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(), colors = authFieldColors(),
            isError = password.isNotEmpty() && !passOk
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirm, onValueChange = { confirm = it; error = null },
            label = { Text("Confirm Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(), colors = authFieldColors(),
            isError = confirm.isNotEmpty() && !matchOk
        )

        if (confirm.isNotEmpty() && !matchOk) {
            Spacer(Modifier.height(6.dp))
            Text("Passwords don't match", color = Color(0xFFFF5C5C), fontSize = 12.sp)
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = Color(0xFFFF5C5C), fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            enabled = canSubmit,
            onClick = {
                scope.launch {
                    loading = true; error = null
                    try {
                        repo.signUp(email, password)
                        onSignedUp()
                    } catch (e: Exception) {
                        error = repo.friendlyError(e)
                    } finally { loading = false }
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5A0))
        ) {
            if (loading) CircularProgressIndicator(
                color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp
            ) else Text(
                "Send Magic Link",
                color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "← Back to sign in",
            color = Color(0xFF00E5A0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onBack
            )
        )
    }
}

/* ═══════════════ VERIFY EMAIL ═══════════════ */
@Composable
fun VerifyEmailScreen(
    email: String,
    onVerified: () -> Unit,
    onSignOut: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AuthRepository() }

    var checking by remember { mutableStateOf(false) }
    var resending by remember { mutableStateOf(false) }
    var pollEnabled by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("Waiting for verification…") }

    // Auto-poll every 3 seconds
    LaunchedEffect(pollEnabled) {
        while (pollEnabled) {
            delay(3000)
            val ok = runCatching { repo.checkVerified() }.getOrDefault(false)
            if (ok) {
                pollEnabled = false
                onVerified()
                return@LaunchedEffect
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xFF0A0E14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(96.dp).background(
                    Color(0x1A00E5A0), RoundedCornerShape(28.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Email, null,
                    tint = Color(0xFF00E5A0), modifier = Modifier.size(46.dp)
                )
            }

            Spacer(Modifier.height(26.dp))
            Text("Verify your email", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Text("We sent a magic link to:", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(email, color = Color(0xFF00E5A0), fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text(
                "Open the email, tap the link, and this screen will unlock automatically.",
                color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp
            )

            Spacer(Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5A0), strokeWidth = 2.dp, modifier = Modifier.size(18.dp)
                )
                Text(status, color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(Modifier.height(32.dp))

            Box(
                Modifier.fillMaxWidth().height(52.dp)
                    .background(Color(0xFF00E5A0), RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (checking) return@clickable
                        scope.launch {
                            checking = true; status = "Checking…"
                            val ok = runCatching { repo.checkVerified() }.getOrDefault(false)
                            if (ok) { pollEnabled = false; onVerified() }
                            else {
                                status = "Not verified yet — check your inbox"
                                Toast.makeText(ctx, "Open the link in your email first", Toast.LENGTH_SHORT).show()
                            }
                            checking = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (checking) CircularProgressIndicator(
                    color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                ) else Text("I've verified ✓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(14.dp))

            Box(
                Modifier.fillMaxWidth().height(50.dp)
                    .border(1.dp, Color(0xFF2A3540), RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (resending) return@clickable
                        scope.launch {
                            resending = true
                            runCatching { repo.resendVerification() }
                                .onSuccess { Toast.makeText(ctx, "Verification email sent again", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(ctx, it.message ?: "Failed", Toast.LENGTH_LONG).show() }
                            resending = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(if (resending) "Sending…" else "Resend email", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Use a different account",
                color = Color.Gray, fontSize = 13.sp,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onSignOut
                )
            )
        }
    }
}
