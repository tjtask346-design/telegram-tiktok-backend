package com.aim.earny.ui.screens.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.R
import com.aim.earny.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(onSignOut: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    val user = auth.currentUser
    var displayName by remember { mutableStateOf(user?.displayName ?: "Earny User") }
    var bio by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(user?.email ?: "") }

    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            runCatching {
                val doc = db.collection("users").document(uid).get().await()
                doc.getString("fullName")?.let { displayName = it }
                doc.getString("bio")?.let { bio = it }
                doc.getString("email")?.let { email = it }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(EarnyBlack).padding(top = 48.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(Gold),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.earny_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(displayName, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (bio.isNotBlank()) {
                    Text(bio, color = TextWhite60, fontSize = 13.sp, maxLines = 1)
                } else {
                    Text(email, color = TextWhite60, fontSize = 12.sp, maxLines = 1)
                }
            }
            Icon(
                Icons.Filled.Logout, "logout",
                tint = TextWhite60,
                modifier = Modifier.size(24.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onSignOut
                )
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Stat("0", "Videos", Modifier.weight(1f))
            Stat("0", "Followers", Modifier.weight(1f))
            Stat("0", "Likes", Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape)
                    .background(EarnyInput),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Videocam, null,
                    tint = TextWhite60,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "No videos yet",
                color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your created videos will appear here",
                color = TextWhite40, fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextWhite60, fontSize = 12.sp)
    }
}
