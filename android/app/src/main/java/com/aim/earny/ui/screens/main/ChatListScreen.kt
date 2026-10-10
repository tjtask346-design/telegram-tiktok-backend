package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.aim.earny.data.Chat
import com.aim.earny.data.ChatRepository
import com.aim.earny.ui.components.SafeAvatar
import com.aim.earny.ui.theme.*

@Composable
fun ChatListScreen(
    onOpenChat: (Chat) -> Unit
) {
    val repo = remember { ChatRepository() }
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        chats = repo.myChats()
        loading = false
    }

    when {
        loading -> Box(
            Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = Gold) }

        chats.isEmpty() -> Column(
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
            Text(
                "No messages yet",
                color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap the 💬 button on someone's profile\nto start a chat",
                color = TextWhite60, fontSize = 13.sp,
                textAlign = TextAlign.Center, lineHeight = 19.sp
            )
        }

        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(chats) { c ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onOpenChat(c) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SafeAvatar(
                        name = c.otherUsername,
                        size = 52.dp,
                        picMsgId = c.otherPicMsgId
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "@" + c.otherUsername,
                            color = TextWhite, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            c.lastMessage.ifBlank { "Say hi 👋" },
                            color = TextWhite60, fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                    if (c.unreadForMe > 0) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(Gold)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                c.unreadForMe.coerceAtMost(99).toString(),
                                color = EarnyBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
