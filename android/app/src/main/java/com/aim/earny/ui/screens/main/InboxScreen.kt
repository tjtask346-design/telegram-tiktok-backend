package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import com.aim.earny.data.InboxMessage
import com.aim.earny.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun InboxScreen() {
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var messages by remember { mutableStateOf<List<InboxMessage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid != null) {
            runCatching {
                // ⚠️ No orderBy(timestamp) — avoids composite index.
                // We sort client-side.
                val snap = db.collection("messages")
                    .whereEqualTo("receiver", uid)
                    .limit(100)
                    .get()
                    .await()
                messages = snap.documents
                    .map { DocumentMapper.message(it) }
                    .sortedByDescending { it.timestamp }
            }
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(EarnyBlack).padding(top = 48.dp)) {
        Text(
            "Inbox",
            color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Activity & messages",
            color = TextWhite60, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(16.dp))

        when {
            loading -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Gold) }

            messages.isEmpty() -> EmptyInbox()

            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(messages) { msg ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(Gold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                msg.fromName.firstOrNull()?.uppercase() ?: "?",
                                color = EarnyBlack, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                msg.fromName,
                                color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                            )
                            Text(msg.text, color = TextWhite60, fontSize = 13.sp, maxLines = 1)
                        }
                        if (msg.unread) {
                            Box(Modifier.size(8.dp).background(HeartRed, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInbox() {
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
                Icons.Filled.Email, null,
                tint = Gold, modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("Inbox is empty", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Messages from other creators\nwill appear here",
            color = TextWhite60, fontSize = 13.sp, lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
    }
}
