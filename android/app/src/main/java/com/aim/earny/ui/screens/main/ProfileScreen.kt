package com.aim.earny.ui.screens.main

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aim.earny.data.AppEvents
import com.aim.earny.data.UserProfile
import com.aim.earny.data.Video
import com.aim.earny.data.formatCount
import com.aim.earny.ui.components.EarnyButton
import com.aim.earny.ui.components.SafeAvatar
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.ProfileTab
import com.aim.earny.vm.ProfileViewModel

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onEditProfile: () -> Unit,
    onAddFriends: () -> Unit,
    onOpenSettings: () -> Unit = {},
    targetUid: String? = null,
    vm: ProfileViewModel = viewModel()
) {
    val profileRefresh by AppEvents.profileRefresh.collectAsStateWithLifecycle()
    LaunchedEffect(targetUid, profileRefresh) {
        runCatching { vm.load(targetUid) }
    }

    val profile by vm.profile.collectAsStateWithLifecycle()
    val videos by vm.videos.collectAsStateWithLifecycle()
    val privateVideos by vm.privateVideos.collectAsStateWithLifecycle()
    val reposts by vm.reposts.collectAsStateWithLifecycle()
    val liked by vm.liked.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val isOwn by vm.isOwnProfile.collectAsStateWithLifecycle()
    val isFollowing by vm.isFollowing.collectAsStateWithLifecycle()
    val isBlocked by vm.isBlocked.collectAsStateWithLifecycle()

    val ctx = LocalContext.current
    var currentTab by remember { mutableStateOf(ProfileTab.VIDEOS) }
    var playingVideo by remember { mutableStateOf<Video?>(null) }
    var pendingDelete by remember { mutableStateOf<Video?>(null) }
    var showProfileActions by remember { mutableStateOf(false) }
    var showReportUser by remember { mutableStateOf(false) }
    var confirmBlockProfile by remember { mutableStateOf(false) }
    var showActionsFor by remember { mutableStateOf<Video?>(null) }

    Column(Modifier.fillMaxSize().background(EarnyBlack)) {

        if (isOwn) {
            ProfileTopBar(
                username = profile?.username.orEmpty(),
                onAddFriends = onAddFriends,
                onOpenSettings = onOpenSettings
            )
        } else {
            // Simple bar for other user's profile
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "@" + (profile?.username ?: ""),
                    color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.MoreVert, "more",
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showProfileActions = true }
                )
            }
        }

        if (loading && profile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gold)
            }
        } else {
            val p = profile ?: UserProfile()

            val list: List<Video> = when (currentTab) {
                ProfileTab.VIDEOS -> videos
                ProfileTab.PRIVATE -> if (isOwn) privateVideos else emptyList()
                ProfileTab.REPOSTS -> reposts
                ProfileTab.LIKED -> liked
                ProfileTab.SAVED -> saved
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileHeader(
                        profile = p,
                        isOwn = isOwn,
                        isFollowing = isFollowing,
                        onEdit = onEditProfile,
                        onShare = {
                            val handle = p.username.ifBlank { "" }
                            val url = if (handle.isBlank()) "https://earny.app"
                                else "https://earny.app/@$handle"
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val disp = if (handle.isBlank()) "this creator" else "@$handle"
                                putExtra(Intent.EXTRA_TEXT,
                                    "Check out $disp on Earny!\n$url")
                            }
                            ctx.startActivity(
                                Intent.createChooser(share, "Share profile")
                            )
                        },
                        onFollow = { vm.toggleFollow() }
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileTabs(
                        current = currentTab,
                        isOwn = isOwn,
                        onSelect = { currentTab = it }
                    )
                }

                if (list.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyTab(tab = currentTab, isOwn = isOwn)
                    }
                } else {
                    items(list) { v -> VideoTile(
                        video = v,
                        onTap = { playingVideo = v },
                        onLongPress = { if (isOwn) showActionsFor = v }
                    ) }
                }
            }
        }
    }

    // Fullscreen player overlay
    playingVideo?.let { v ->
        VideoPlayerOverlay(
            video = v,
            onClose = { playingVideo = null }
        )
    }

    // Long-press action sheet
    showActionsFor?.let { v ->
        VideoActionsSheet(
            onDismiss = { showActionsFor = null },
            onPlay = {
                playingVideo = v
                showActionsFor = null
            },
            onDelete = {
                pendingDelete = v
                showActionsFor = null
            }
        )
    }

    // Other-profile actions
    if (showProfileActions) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showProfileActions = false },
            containerColor = EarnySurface,
            dragHandle = {
                androidx.compose.material3.BottomSheetDefaults.DragHandle(color = TextWhite40)
            }
        ) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showProfileActions = false
                            showReportUser = true
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Flag, null, tint = Color(0xFFFF9800), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Report", color = Color(0xFFFF9800), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showProfileActions = false
                            confirmBlockProfile = true
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Block, null, tint = Color(0xFFE53935), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        if (isBlocked) "Unblock user" else "Block user",
                        color = Color(0xFFE53935), fontSize = 15.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showReportUser) {
        val reasons = listOf("spam", "harassment", "nudity", "violence", "hate_speech", "other")
        var reason by remember { mutableStateOf(reasons.first()) }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportUser = false },
            containerColor = EarnySurface,
            title = { Text("Report user", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Why are you reporting?", color = TextWhite60, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    reasons.forEach { r ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { reason = r }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = reason == r,
                                onClick = { reason = r },
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                    selectedColor = Gold, unselectedColor = TextWhite40
                                )
                            )
                            Text(
                                r.replace("_", " ").replaceFirstChar { it.uppercase() },
                                color = TextWhite, fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 200) note = it },
                        label = { Text("Note (optional)", color = TextWhite40) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = EarnyBorder,
                            cursorColor = Gold
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uid = profile?.uid ?: ""
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            com.aim.earny.data.ReportRepository().reportUser(uid, reason, note)
                        }
                    }
                    showReportUser = false
                }) {
                    Text("Submit", color = Gold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportUser = false }) {
                    Text("Cancel", color = TextWhite)
                }
            }
        )
    }

    if (confirmBlockProfile) {
        AlertDialog(
            onDismissRequest = { confirmBlockProfile = false },
            containerColor = EarnySurface,
            title = { Text(if (isBlocked) "Unblock user?" else "Block user?", color = TextWhite) },
            text = {
                Text(
                    if (isBlocked) "They'll appear again in your feed."
                    else "Their videos will be hidden. They won't be notified.",
                    color = TextWhite60
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.toggleBlock()
                    confirmBlockProfile = false
                }) {
                    Text(
                        if (isBlocked) "Unblock" else "Block",
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBlockProfile = false }) {
                    Text("Cancel", color = TextWhite)
                }
            }
        )
    }

    // Delete confirmation
    pendingDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = EarnySurface,
            title = { Text("Delete video?", color = TextWhite) },
            text = {
                Text(
                    "This will permanently remove the video. This cannot be undone.",
                    color = TextWhite60
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteVideo(v)
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VideoActionsSheet(
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = EarnySurface,
        dragHandle = {
            androidx.compose.material3.BottomSheetDefaults.DragHandle(color = TextWhite40)
        }
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = onPlay
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PlayArrow, null,
                    tint = TextWhite, modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text("Play video", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            Row(
                Modifier.fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = onDelete
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Delete, null,
                    tint = Color(0xFFE53935), modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Delete video",
                    color = Color(0xFFE53935),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ProfileTopBar(
    username: String,
    onAddFriends: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (username.isBlank()) "@username" else "@$username",
            color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Filled.KeyboardArrowDown, null,
            tint = TextWhite, modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.weight(1f))

        Icon(
            Icons.Filled.PersonAdd, "add-friends",
            tint = TextWhite,
            modifier = Modifier
                .size(26.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onAddFriends
                )
        )
        Spacer(Modifier.width(14.dp))
        Icon(
            Icons.Filled.Settings, "settings",
            tint = TextWhite,
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onOpenSettings
                )
        )
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    isOwn: Boolean,
    isFollowing: Boolean,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onFollow: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            SafeAvatar(
                name = profile.username.ifBlank {
                    profile.fullName.ifBlank { profile.email }
                },
                size = 100.dp,
                picMsgId = profile.profilePicMsgId
            )
            Box(
                Modifier
                    .size(26.dp)
                    .background(Gold, CircleShape)
                    .border(3.dp, EarnyBlack, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = onEdit
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Add, null,
                    tint = EarnyBlack,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "@" + profile.username.ifBlank { "username" },
                color = TextWhite, fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (profile.verified) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.Verified, "verified",
                    tint = Color(0xFF1DA1F2),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth()) {
            StatBox(formatCount(profile.following), "Following", Modifier.weight(1f))
            StatBox(formatCount(profile.followers), "Followers", Modifier.weight(1f))
            StatBox(formatCount(profile.totalLikes), "Likes", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        if (isOwn) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallOutlineButton("Edit profile", Modifier.weight(1f), onEdit)
                SmallOutlineButton("Share profile", Modifier.weight(1f), onShare)
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    EarnyButton(
                        text = if (isFollowing) "Following" else "Follow",
                        gradient = !isFollowing,
                        onClick = onFollow
                    )
                }
                SmallIconButton(Icons.Filled.Email)
                SmallIconButton(Icons.Filled.Share)
            }
        }

        Spacer(Modifier.height(14.dp))

        if (profile.bio.isNotBlank() || profile.link.isNotBlank()) {
            com.aim.earny.ui.screens.profile.BioRenderer(
                bio = profile.bio,
                link = profile.link
            )
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextWhite60, fontSize = 12.sp)
    }
}

@Composable
private fun SmallOutlineButton(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EarnySurface)
            .border(1.dp, EarnyBorder, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SmallIconButton(icon: ImageVector) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EarnySurface)
            .border(1.dp, EarnyBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = TextWhite, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileTabs(
    current: ProfileTab,
    isOwn: Boolean,
    onSelect: (ProfileTab) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth().height(44.dp)) {
            TabItem(Icons.Filled.GridView, current == ProfileTab.VIDEOS, Modifier.weight(1f)) {
                onSelect(ProfileTab.VIDEOS)
            }
            if (isOwn) {
                TabItem(Icons.Filled.Lock, current == ProfileTab.PRIVATE, Modifier.weight(1f)) {
                    onSelect(ProfileTab.PRIVATE)
                }
            }
            TabItem(Icons.Filled.Repeat, current == ProfileTab.REPOSTS, Modifier.weight(1f)) {
                onSelect(ProfileTab.REPOSTS)
            }
            TabItem(Icons.Filled.Favorite, current == ProfileTab.LIKED, Modifier.weight(1f)) {
                onSelect(ProfileTab.LIKED)
            }
            if (isOwn) {
                TabItem(Icons.Filled.Bookmark, current == ProfileTab.SAVED, Modifier.weight(1f)) {
                    onSelect(ProfileTab.SAVED)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(EarnyBorder))
    }
}

@Composable
private fun TabItem(
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null,
            tint = if (selected) TextWhite else TextWhite40,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun VideoTile(
    video: Video,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
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
                indication = null, onClick = onTap
            )
            .pointerInput(video.id) {
                detectTapGestures(
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        // Show thumbnail if available; otherwise just the play icon
        if (video.thumbB64.isNotBlank()) {
            com.aim.earny.ui.components.VideoThumbBase64(
                b64 = video.thumbB64,
                modifier = Modifier.fillMaxSize(),
                showPlayIcon = true
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.PlayArrow, null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (video.isPinned) {
            Row(
                Modifier.align(Alignment.TopStart).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PushPin, null,
                    tint = Color.White, modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "Pinned", color = Color.White,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            Modifier.align(Alignment.BottomStart).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.PlayArrow, null,
                tint = Color.White, modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                formatCount(video.views),
                color = Color.White, fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyTab(tab: ProfileTab, isOwn: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon: ImageVector = when (tab) {
            ProfileTab.VIDEOS -> Icons.Filled.Videocam
            ProfileTab.PRIVATE -> Icons.Filled.Lock
            ProfileTab.REPOSTS -> Icons.Filled.Repeat
            ProfileTab.LIKED -> Icons.Filled.Favorite
            ProfileTab.SAVED -> Icons.Filled.Bookmark
        }
        val title = when (tab) {
            ProfileTab.VIDEOS -> "No videos yet"
            ProfileTab.PRIVATE -> "No private videos"
            ProfileTab.REPOSTS -> "No reposts yet"
            ProfileTab.LIKED -> "No liked videos"
            ProfileTab.SAVED -> "No saved videos"
        }
        val sub = when (tab) {
            ProfileTab.VIDEOS -> if (isOwn) "Tap Earny Orb to post" else "This user hasn't posted"
            ProfileTab.PRIVATE -> "Only you can see these"
            ProfileTab.REPOSTS -> "Videos you repost will appear here"
            ProfileTab.LIKED -> "Videos you like appear here"
            ProfileTab.SAVED -> "Videos you bookmark appear here"
        }

        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(EarnyInput),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = TextWhite60, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            sub, color = TextWhite60, fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
