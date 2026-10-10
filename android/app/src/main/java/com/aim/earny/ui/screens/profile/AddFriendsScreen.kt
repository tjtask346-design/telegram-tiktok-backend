package com.aim.earny.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.data.DocumentMapper
import com.aim.earny.data.UserProfile
import com.aim.earny.data.formatCount
import com.aim.earny.ui.components.SafeAvatar
import com.aim.earny.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun AddFriendsScreen(onBack: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    val me = FirebaseAuth.getInstance().currentUser?.uid

    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var following by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(me) {
        runCatching {
            val snap = db.collection("users").limit(100).get().await()
            users = snap.documents
                .map { DocumentMapper.user(it) }
                .filter { it.uid != me }
                .sortedByDescending { it.followers }
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(EarnyBlack)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ArrowBack, "back",
                tint = TextWhite,
                modifier = Modifier.size(24.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onBack
                )
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "Add friends",
                color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
        }

        when {
            loading -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Gold) }

            users.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.size(72.dp).clip(
                        RoundedCornerShape(24.dp)
                    ).background(Gold.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PersonAdd, null,
                        tint = Gold, modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "No other users yet",
                    color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Invite friends to join Earny",
                    color = TextWhite60, fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(users) { user ->
                    UserRow(
                        user = user,
                        isFollowing = following.contains(user.uid),
                        onToggle = {
                            val uid = user.uid
                            val nowFollowing = !following.contains(uid)
                            following = if (nowFollowing)
                                following + uid else following - uid
                            // Fire-and-forget updates
                            if (me != null) {
                                db.collection("users").document(uid).update(
                                    "followers",
                                    FieldValue.increment(if (nowFollowing) 1 else -1)
                                )
                                db.collection("users").document(me).update(
                                    "following",
                                    FieldValue.increment(if (nowFollowing) 1 else -1)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserRow(
    user: UserProfile,
    isFollowing: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SafeAvatar(
            name = user.username.ifBlank {
                user.fullName.ifBlank { user.email }
            },
            size = 52.dp
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                user.username.ifBlank { user.fullName.ifBlank { "user" } },
                color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
            Text(
                "${formatCount(user.followers)} followers",
                color = TextWhite60, fontSize = 12.sp
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (isFollowing) EarnySurface else Gold)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onToggle
                )
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Text(
                if (isFollowing) "Following" else "Follow",
                color = if (isFollowing) TextWhite else EarnyBlack,
                fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}
