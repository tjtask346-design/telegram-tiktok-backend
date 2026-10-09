package com.aim.earny.ui.screens.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.R
import com.aim.earny.data.Video
import com.aim.earny.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(onSignOut: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val user = auth.currentUser
    val uid = user?.uid

    var displayName by remember { mutableStateOf(user?.displayName ?: "Earny User") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var loadingVideos by remember { mutableStateOf(true) }
    var totalLikes by remember { mutableStateOf(0L) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect

        // Load user profile
        runCatching {
            val doc = db.collection("users").document(uid).get().await()
            doc.getString("fullName")?.let { displayName = it }
            doc.getString("username")?.let { username = it }
            doc.getString("bio")?.let { bio = it }
            doc.getString("email")?.let { email = it }
        }

        // Load user's videos
        runCatching {
            val snap = db.collection("videos")
                .whereEqualTo("uploader", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(60).get().await()
            videos = snap.documents.mapNotNull { d ->
                d.toObject(Video::class.java)?.copy(id = d.id)
            }
            totalLikes = videos.sumOf { it.likes }
        }
        loadingVideos = false
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
                if (username.isNotBlank()) {
                    Text("@$username", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (bio.isNotBlank()) {
                    Text(bio, color = TextWhite60, fontSize = 12.sp, maxLines = 1)
                } else {
                    Text(email, color = TextWhite60, fontSize = 11.sp, maxLines = 1)
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
            Stat(videos.size.toString(), "Videos", Modifier.weight(1f))
            Stat("0", "Followers", Modifier.weight(1f))
            Stat(formatCount(totalLikes), "Likes", Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        when {
            loadingVideos -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Gold) }

            videos.isEmpty() -> EmptyProfile()

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videos) { video ->
                    VideoTile(video)
                }
            }
        }
    }
}

@Composable
private fun VideoTile(video: Video) {
    val gradients = listOf(GradPurplePink, GradBlueCyan, GradOrangeRed, GradGreenTeal, GradGoldOrange)
    val grad = gradients[video.id.hashCode().let { if (it < 0) -it else it } % gradients.size]

    Box(
        Modifier.fillMaxWidth().aspectRatio(0.75f)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(grad)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.PlayArrow, null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(36.dp)
        )
        Box(
            Modifier.fillMaxWidth().align(Alignment.BottomStart)
                .background(Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                ))
                .padding(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                    formatCount(video.likes),
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyProfile() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(80.dp).clip(CircleShape).background(EarnyInput),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Videocam, null,
                tint = TextWhite60,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("No videos yet", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap the Earny Orb to post your first video",
            color = TextWhite60, fontSize = 13.sp,
            textAlign = TextAlign.Center, lineHeight = 19.sp
        )
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextWhite60, fontSize = 12.sp)
    }
}

private fun formatCount(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}
