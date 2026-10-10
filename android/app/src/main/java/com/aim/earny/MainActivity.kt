package com.aim.earny

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.data.FcmRepository
import com.aim.earny.navigation.EarnyNavGraph
import com.aim.earny.navigation.Routes
import com.aim.earny.notifications.NotificationHelper
import com.aim.earny.ui.theme.EarnyBlack
import com.aim.earny.ui.theme.EarnySurface
import com.aim.earny.ui.theme.EarnyTheme
import com.aim.earny.ui.theme.Gold
import com.aim.earny.ui.theme.TextWhite
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Global crash logger → SharedPreferences
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            try {
                val prefs = getSharedPreferences("earny_crash", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("last_error", android.util.Log.getStackTraceString(ex))
                    .putLong("when", System.currentTimeMillis())
                    .apply()
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, ex)
        }

        setContent {
            EarnyTheme { RootApp() }
        }
    }
}

@Composable
fun RootApp() {
    val ctx = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var ready by remember { mutableStateOf(false) }
    var startRoute by remember { mutableStateOf(Routes.SPLASH) }
    var crashText by remember { mutableStateOf<String?>(null) }

    // Check for previous crash on launch
    LaunchedEffect(Unit) {
        val prefs = ctx.getSharedPreferences("earny_crash", Context.MODE_PRIVATE)
        val last = prefs.getString("last_error", null)
        if (!last.isNullOrBlank()) {
            crashText = last
            // Don't remove yet — remove when user dismisses
        }
    }

    LaunchedEffect(Unit) {
        delay(150)
        runCatching { NotificationHelper.ensureChannel(ctx) }
        if (auth.currentUser != null) {
            launch { runCatching { FcmRepository().registerToken() } }
        }
        startRoute = try {
            val u = auth.currentUser
            when {
                u == null -> Routes.SPLASH
                !u.isEmailVerified -> Routes.SPLASH
                else -> Routes.MAIN
            }
        } catch (_: Throwable) {
            Routes.SPLASH
        }
        ready = true
    }

    if (!ready) {
        Box(
            Modifier.fillMaxSize().background(EarnyBlack),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = Gold) }
    } else {
        EarnyNavGraph(startRoute = startRoute, onSignedOut = {})
    }

    // ---- Crash modal ----
    crashText?.let { error ->
        CrashDialog(
            text = error,
            onDismiss = {
                ctx.getSharedPreferences("earny_crash", Context.MODE_PRIVATE)
                    .edit().remove("last_error").apply()
                crashText = null
            }
        )
    }
}

@Composable
private fun CrashDialog(
    text: String,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = { /* force button click */ },
        containerColor = EarnySurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    "⚠️ Crash Report",
                    color = Color(0xFFFF5C5C),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Previous session crashed. Copy the message and send to developer.",
                    color = TextWhite.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .background(
                        Color.Black.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = text,
                    color = Color(0xFFB0FFB0),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(text))
                onDismiss()
            }) {
                Text("Copy & Dismiss", color = Gold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = TextWhite.copy(alpha = 0.7f))
            }
        }
    )
}
