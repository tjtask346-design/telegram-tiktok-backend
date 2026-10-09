package com.aim.earny

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.*
import com.aim.earny.ui.screens.*
import com.aim.earny.ui.theme.EarnyTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    object AwaitingVerification : AuthState()
    object LoggedIn : AuthState()
}

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
    var state by remember { mutableStateOf<AuthState>(AuthState.Loading) }

    LaunchedEffect(Unit) {
        delay(250)
        val u = auth.currentUser
        state = when {
            u == null -> AuthState.LoggedOut
            !u.isEmailVerified -> AuthState.AwaitingVerification
            else -> AuthState.LoggedIn
        }
    }

    when (state) {
        AuthState.Loading -> Box(
            Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = Color(0xFF00E5A0)) }

        AuthState.LoggedOut -> AuthFlow(
            onAuthed = {
                val u = auth.currentUser
                state = if (u?.isEmailVerified == true)
                    AuthState.LoggedIn else AuthState.AwaitingVerification
            }
        )

        AuthState.AwaitingVerification -> VerifyEmailScreen(
            email = auth.currentUser?.email ?: "",
            onVerified = { state = AuthState.LoggedIn },
            onSignOut = { auth.signOut(); state = AuthState.LoggedOut }
        )

        AuthState.LoggedIn -> MainNav(
            onSignOut = { auth.signOut(); state = AuthState.LoggedOut }
        )
    }
}

@Composable
fun AuthFlow(onAuthed: () -> Unit) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoggedIn = onAuthed,
                onGoSignup = { nav.navigate("signup") }
            )
        }
        composable("signup") {
            SignupScreen(
                onSignedUp = onAuthed,
                onBack = { nav.popBackStack() }
            )
        }
    }
}

@Composable
fun MainNav(onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF111111)) {
                NavigationBarItem(
                    selected = current == "feed",
                    onClick = { nav.navigate("feed") { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Home, "Feed") },
                    label = { Text("Feed") }
                )
                NavigationBarItem(
                    selected = current == "upload",
                    onClick = { nav.navigate("upload") { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Add, "Upload") },
                    label = { Text("Upload") }
                )
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "feed", modifier = Modifier.padding(pad)) {
            composable("feed") { FeedScreen(onSignOut = onSignOut) }
            composable("upload") { UploadScreen(onDone = { nav.navigate("feed") }) }
        }
    }
}
