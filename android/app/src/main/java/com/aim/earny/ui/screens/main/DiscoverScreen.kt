package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.data.DocumentMapper
import com.aim.earny.data.UserProfile
import com.aim.earny.data.formatCount
import com.aim.earny.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun DiscoverScreen(onOpenProfile: (String) -> Unit = {}) {
    val db = remember { FirebaseFirestore.getInstance() }
    val me = FirebaseAuth.getInstance().currentUser?.uid
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var lastDoc by remember { mutableStateOf<DocumentSnapshot?>(null) }

    val PAGE_SIZE = 30L

    suspend fun loadPage(reset: Boolean) {
        if (reset) {
            loading = true
            users = emptyList()
            lastDoc = null
            hasMore = true
        } else {
            loadingMore = true
        }
        runCatching {
            var q = db.collection("users")
                .orderBy("followers", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
            lastDoc?.let { q = q.startAfter(it) }
            val snap = q.get().await()
            val fresh = snap.documents
                .map { DocumentMapper.user(it) }
                .filter { it.uid != me }
            users = if (reset) fresh else users + fresh
            lastDoc = snap.documents.lastOrNull() ?: lastDoc
            hasMore = snap.size() >= PAGE_SIZE
        }
        loading = false
        loadingMore = false
    }

    LaunchedEffect(me) { loadPage(true) }

    Column(Modifier.fillMaxSize().background(EarnyBlack).padding(top = 48.dp)) {
        Text(
            "Discover",
            color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Creators on Earny",
            color = TextWhite60, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(16.dp))

        when {
            loading -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Gold) }

            users.isEmpty() -> EmptyFriends()

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(users) { user ->
                    UserCard(user, onClick = { onOpenProfile(user.uid) })
                }

                // Loading indicator
                if (loadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Gold, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // Trigger load more when reaching end
                if (hasMore && !loadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LaunchedEffect(users.size) {
                            loadPage(false)
                        }
                        Box(Modifier.height(1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCard(user: UserProfile, onClick: () -> Unit) {
    val name = user.username.ifBlank {
        user.fullName.ifBlank { user.email.substringBefore("@") }
    }
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EarnySurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape)
                .background(Brush.sweepGradient(listOf(Gold, Orange, Gold))),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(58.dp).clip(CircleShape).background(EarnyInput),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.uppercase() ?: "?",
                    color = Gold, fontSize = 24.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "@" + name, color = TextWhite, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${formatCount(user.followers)} followers",
            color = TextWhite60, fontSize = 11.sp
        )
    }
}

@Composable
private fun EmptyFriends() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape)
                .background(Gold.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Person, null,
                tint = Gold, modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("No creators yet", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Invite friends to join Earny\nand start earning together",
            color = TextWhite60, fontSize = 13.sp, lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
    }
}
