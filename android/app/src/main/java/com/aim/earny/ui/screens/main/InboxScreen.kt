package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.data.AppNotification
import com.aim.earny.data.NotificationsRepository
import com.aim.earny.ui.components.SafeAvatar
import com.aim.earny.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun InboxScreen(
    onOpenProfile: (String) -> Unit = {},
    onOpenChat: (com.aim.earny.data.Chat) -> Unit = {}
) {
    val repo = remember { NotificationsRepository() }
    val scope = rememberCoroutineScope()

    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(0) }

    suspend fun reload() {
        loading = true
        notifications = repo.load()
        loading = false
    }

    LaunchedEffect(Unit) {
        reload()
        kotlinx.coroutines.delay(1000)
        repo.markAllRead()
    }

    Column(Modifier.fillMaxSize().background(EarnyBlack).padding(top = 48.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Inbox", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (notifications.isNotEmpty()) {
                TextButton(onClick = {
                    scope.launch {
                        repo.clearAll()
                        notifications = emptyList()
                    }
                }) {
                    Text("Clear all", color = TextWhite60, fontSize = 12.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            TabHeader("Activity", selected == 0, Modifier.weight(1f)) { selected = 0 }
            TabHeader("Messages", selected == 1, Modifier.weight(1f)) { selected = 1 }
        }
        Box(Modifier.fillMaxSize()) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Gold)
                }
                selected == 0 -> {
                    if (notifications.isEmpty()) {
                        EmptyState(Icons.Filled.Notifications, "No activity yet",
                            "Likes, comments and follows\nwill appear here")
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(notifications) { n ->
                                NotificationRow(
                                    n = n,
                                    onClick = { if (n.senderUid.isNotBlank()) onOpenProfile(n.senderUid) },
                                    onLongPress = {
                                        scope.launch {
                                            repo.deleteNotification(n.id)
                                            notifications = notifications.filter { it.id != n.id }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                else -> ChatListScreen(onOpenChat = onOpenChat)
            }
        }
    }
}

@Composable
private fun TabHeader(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, onClick = onClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label,
            color = if (selected) TextWhite else TextWhite60,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(if (selected) 32.dp else 0.dp).height(2.dp)
            .background(if (selected) TextWhite else Color.Transparent))
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit, onLongPress: () -> Unit) {
    val iconVec = when (n.kind) {
        "like" -> Icons.Filled.Favorite
        "comment" -> Icons.Filled.ChatBubble
        "follow" -> Icons.Filled.PersonAdd
        else -> Icons.Filled.Notifications
    }
    val iconColor = when (n.kind) {
        "like" -> HeartRed
        "comment" -> Color(0xFF1DA1F2)
        "follow" -> Gold
        else -> TextWhite60
    }
    Row(
        Modifier.fillMaxWidth()
            .background(if (n.unread) Gold.copy(alpha = 0.05f) else Color.Transparent)
            .pointerInput(n.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SafeAvatar(name = n.senderName.ifBlank { "?" }, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconVec, null, tint = iconColor, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(n.title.ifBlank { "Earny" }, color = TextWhite, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(n.body, color = TextWhite60, fontSize = 12.sp, maxLines = 2)
        }
        if (n.unread) Box(Modifier.size(8.dp).clip(CircleShape).background(Gold))
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, sub: String
) {
    Column(Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(Gold.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(sub, color = TextWhite60, fontSize = 13.sp, lineHeight = 19.sp,
            textAlign = TextAlign.Center)
    }
}
