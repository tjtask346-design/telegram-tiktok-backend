@file:OptIn(ExperimentalMaterial3Api::class)

package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.data.UserProfile
import com.aim.earny.data.UserSearchRepository
import com.aim.earny.data.Video
import com.aim.earny.data.VideoSearchRepository
import com.aim.earny.data.formatCount
import com.aim.earny.ui.components.SafeAvatar
import com.aim.earny.ui.components.VideoThumbBase64
import com.aim.earny.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UserSearchScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenVideo: (Video) -> Unit = {},
    onOpenHashtag: (String) -> Unit = {}
) {
    val userRepo = remember { UserSearchRepository() }
    val videoRepo = remember { VideoSearchRepository() }

    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(1) } // 0 = Users, 1 = Videos

    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }

    // Debounced search
    LaunchedEffect(query, tab) {
        val q = query.trim()
        if (q.length < 2) {
            users = emptyList()
            videos = emptyList()
            return@LaunchedEffect
        }
        touched = true
        searching = true
        delay(400)
        runCatching {
            if (tab == 0) {
                users = userRepo.search(q.removePrefix("@"))
            } else {
                videos = videoRepo.search(q)
            }
        }
        searching = false
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
            OutlinedTextField(
                value = query,
                onValueChange = { if (it.length <= 50) query = it },
                placeholder = {
                    Text(
                        if (tab == 0) "Search users…"
                        else "Search videos or #tags…",
                        color = TextWhite40
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search, null,
                        tint = TextWhite40, modifier = Modifier.size(18.dp)
                    )
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = EarnyBorder,
                    cursorColor = Gold,
                    focusedContainerColor = EarnyInput,
                    unfocusedContainerColor = EarnyInput
                ),
                shape = RoundedCornerShape(100.dp)
            )
        }

        // Tabs
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
            SearchTab("Users", tab == 0, Modifier.weight(1f)) { tab = 0 }
            SearchTab("Videos", tab == 1, Modifier.weight(1f)) { tab = 1 }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                searching -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Gold) }

                query.trim().length < 2 -> SearchHint(
                    "Type 2+ characters",
                    if (tab == 1) "Try \"#earny\" or a keyword" else "Type a username"
                )

                tab == 0 -> {
                    if (users.isEmpty() && touched) {
                        SearchHint("No users found", "for \"${query.trim()}\"")
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(users) { user ->
                                UserResultRow(user) { onOpenProfile(user.uid) }
                            }
                        }
                    }
                }

                else -> {
                    if (videos.isEmpty() && touched) {
                        SearchHint("No videos found", "for \"${query.trim()}\"")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(videos) { v ->
                                VideoSearchTile(v, onClick = { onOpenVideo(v) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTab(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, onClick = onClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = if (selected) TextWhite else TextWhite60,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.width(if (selected) 28.dp else 0.dp).height(2.dp)
                .background(if (selected) TextWhite else Color.Transparent)
        )
    }
}

@Composable
private fun UserResultRow(user: UserProfile, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SafeAvatar(
            name = user.username.ifBlank {
                user.fullName.ifBlank { user.email }
            },
            size = 52.dp,
            picMsgId = user.profilePicMsgId
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "@" + user.username.ifBlank {
                    user.fullName.ifBlank { "user" }
                },
                color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
            Text(
                "${formatCount(user.followers)} followers",
                color = TextWhite60, fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun VideoSearchTile(video: Video, onClick: () -> Unit) {
    val gradients = listOf(
        GradPurplePink, GradBlueCyan, GradOrangeRed,
        GradGreenTeal, GradGoldOrange
    )
    val idx = video.id.hashCode().let { if (it < 0) -it else it } % gradients.size
    val grad = gradients[idx]

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(grad))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
    ) {
        if (video.thumbB64.isNotBlank()) {
            VideoThumbBase64(
                b64 = video.thumbB64,
                modifier = Modifier.fillMaxSize(),
                showPlayIcon = false
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.PlayArrow, null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Row(
            Modifier.align(Alignment.BottomStart).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.PlayArrow, null,
                tint = Color.White, modifier = Modifier.size(10.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                formatCount(video.views),
                color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SearchHint(line1: String, line2: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Filled.Search, null,
                tint = Gold.copy(alpha = 0.6f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                line1,
                color = TextWhite, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                line2,
                color = TextWhite60, fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
