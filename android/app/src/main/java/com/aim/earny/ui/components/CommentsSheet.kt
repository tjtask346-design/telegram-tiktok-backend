@file:OptIn(ExperimentalMaterial3Api::class)

package com.aim.earny.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aim.earny.data.Comment
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.CommentsViewModel

@Composable
fun CommentsSheet(
    videoId: String,
    onDismiss: () -> Unit,
    vm: CommentsViewModel = viewModel()
) {
    val comments by vm.comments.collectAsState()
    val loading by vm.loading.collectAsState()
    val sending by vm.sending.collectAsState()
    val error by vm.error.collectAsState()

    var draft by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Comment?>(null) }

    LaunchedEffect(videoId) { vm.load(videoId) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = EarnySurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextWhite40) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(bottom = 12.dp)
        ) {
            // Header
            Text(
                "${comments.size} comments",
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )

            Divider(color = EarnyBorder)

            // List
            Box(Modifier.weight(1f)) {
                when {
                    loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Gold) }

                    comments.isEmpty() -> Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "No comments yet",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Be the first to comment",
                            color = TextWhite60,
                            fontSize = 13.sp
                        )
                    }

                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(comments) { c ->
                            CommentRow(
                                comment = c,
                                onLongPress = { if (c.isMine) pendingDelete = c }
                            )
                        }
                    }
                }
            }

            if (error != null) {
                Text(
                    error!!,
                    color = Color(0xFFE53935),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Divider(color = EarnyBorder)

            // Input row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(100.dp))
                        .background(EarnyInput)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 300) draft = it },
                        singleLine = false,
                        maxLines = 4,
                        textStyle = TextStyle(
                            color = TextWhite,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(Gold),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth()) {
                                if (draft.isEmpty()) {
                                    Text(
                                        "Add a comment…",
                                        color = TextWhite40,
                                        fontSize = 14.sp
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (draft.isBlank() || sending) EarnyBorder
                            else Gold
                        )
                        .clickable(enabled = draft.isNotBlank() && !sending) {
                            val t = draft
                            draft = ""
                            vm.send(t)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            color = EarnyBlack,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = if (draft.isBlank()) TextWhite40 else EarnyBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Delete confirm
    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = EarnySurface,
            title = { Text("Delete comment?", color = TextWhite) },
            text = {
                Text(
                    c.text,
                    color = TextWhite60,
                    maxLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(c)
                    pendingDelete = null
                }) {
                    Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = TextWhite)
                }
            }
        )
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    onLongPress: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .pointerInput(comment.id) {
                detectTapGestures(onLongPress = { onLongPress() })
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        SafeAvatar(name = comment.username, size = 36.dp)

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "@${comment.username}",
                    color = TextWhite60,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (comment.isMine) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "You",
                        color = Gold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                comment.text,
                color = TextWhite,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
        }
    }
}
