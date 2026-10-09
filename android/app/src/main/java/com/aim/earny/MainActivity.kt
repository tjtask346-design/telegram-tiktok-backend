package com.aim.earny

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.aim.earny.navigation.EarnyNavGraph
import com.aim.earny.ui.theme.EarnyTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val startSignedIn = auth.currentUser?.isEmailVerified == true

        // Handle magic link deep link
        intent?.data?.let { uri ->
            if (uri.scheme == "https" && uri.host == "earny.app") {
                val link = uri.toString()
                val email = getSharedPreferences("earny", MODE_PRIVATE)
                    .getString("pending_email", null)
                if (email != null && auth.isSignInWithEmailLink(link)) {
                    auth.signInWithEmailLink(email, link)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                getSharedPreferences("earny", MODE_PRIVATE)
                                    .edit().remove("pending_email").apply()
                            }
                        }
                }
            }
        }

        setContent {
            EarnyTheme {
                var signedIn by remember { mutableStateOf(startSignedIn) }
                EarnyNavGraph(
                    startSignedIn = signedIn,
                    onSignOutRequest = { signedIn = false }
                )
            }
        }
    }
}
