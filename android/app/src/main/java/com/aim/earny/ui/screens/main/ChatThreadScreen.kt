package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.data.ChatMessage
import com.aim.earny.data.ChatRepository
import com.aim.earny.ui.components.SafeAvatar
import com.aim.earny.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatThreadScreen(
    chatId: String,
    otherUid: String,
    otherUsername: String,
    otherPicMsgId: Long,
    onBack: () -> Unit
) {
    val repo = remember { ChatRepository() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(chatId) {
        repo.messagesStream(chatId).collect {
            messages = it
            loading = false
        }
    }

    // Auto-scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Mark read
    LaunchedEffect(chatId, messages.size) {
        repo.markRead(chatId)
    }

    Column(Modifier.fillMaxSize().background(EarnyBlack)) {

        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
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
            Spacer(Modifier.width(12.dp))
            SafeAvatar(name = otherUsername, size = 36.dp, picMsgId = otherPicMsgId)
            Spacer(Modifier.width(10.dp))
            Text(
                "@$otherUsername",
                color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold
            )
        }

        HorizontalHorizontalHorizontalDivider(color = EarnyBorder)

        // Messages
        Box(Modifier.weight(1f)) {
            when {
                loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Gold) }

                messages.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Say hi to @$otherUsername 👋",
                        color = TextWhite60, fontSize = 14.sp
                    )
                }

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.size) { i ->
                        MessageBubble(messages[i])
                    }
                }
            }
        }

        HorizontalHorizontalHorizontalDivider(color = EarnyBorder)

        // Input
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(100.dp))
                    .background(EarnyInput)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 500) draft = it },
                    textStyle = TextStyle(color = TextWhite, fontSize = 14.sp),
                    cursorBrush = SolidColor(Gold),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()) {
                            if (draft.isEmpty()) {
                                Text(
                                    "Message…",
                                    color = TextWhite40, fontSize = 14.sp
                                )
                            }
                            inner()
                        }
                    }
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(if (draft.isBlank() || sending) EarnyBorder else Gold)
                    .clickable(enabled = draft.isNotBlank() && !sending) {
                        val t = draft
                        draft = ""
                        sending = true
                        scope.launch {
                            try { repo.send(chatId, otherUid, t) } catch (_: Exception) {}
                            sending = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        color = EarnyBlack, strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        Icons.Filled.Send, "send",
                        tint = if (draft.isBlank()) TextWhite40 else EarnyBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .widthIn(max = 260.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (msg.isMine) 18.dp else 4.dp,
                        bottomEnd = if (msg.isMine) 4.dp else 18.dp
                    )
                )
                .background(if (msg.isMine) Gold else EarnySurface)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                msg.text,
                color = if (msg.isMine) EarnyBlack else TextWhite,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
        }
    }
}
