package com.aim.earny

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aim.earny.navigation.EarnyNavGraph
import com.aim.earny.navigation.Routes
import com.aim.earny.ui.theme.EarnyBlack
import com.aim.earny.ui.theme.EarnyTheme
import com.aim.earny.ui.theme.Gold
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EarnyTheme { RootApp() }
        }
    }
}

@Composable
fun RootApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var ready by remember { mutableStateOf(false) }
    var startRoute by remember { mutableStateOf(Routes.SPLASH) }

    LaunchedEffect(Unit) {
        delay(150)
        // CRITICAL: only use simple routes that match their pattern
        // to avoid NavHost crash from resolved-route mismatch.
        startRoute = try {
            val u = auth.currentUser
            when {
                u == null -> Routes.SPLASH
                !u.isEmailVerified -> Routes.SPLASH  // will navigate to verify from splash
                else -> Routes.MAIN
            }
        } catch (t: Throwable) {
            Routes.SPLASH
        }
        ready = true
    }

    if (!ready) {
        Box(
            Modifier.fillMaxSize().background(EarnyBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Gold)
        }
    } else {
        EarnyNavGraph(
            startRoute = startRoute,
            onSignedOut = { }
        )
    }
}
