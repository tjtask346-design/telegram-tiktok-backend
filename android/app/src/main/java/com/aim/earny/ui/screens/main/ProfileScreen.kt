@file:OptIn(ExperimentalMaterial3Api::class)

package com.aim.earny.ui.screens.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aim.earny.R
import com.aim.earny.data.UserProfile
import com.aim.earny.data.Video
import com.aim.earny.data.formatCount
import com.aim.earny.ui.components.EarnyButton
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.ProfileTab
import com.aim.earny.vm.ProfileViewModel
import SpanStyle
import buildAnnotatedString

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    targetUid: String? = null,
    vm: ProfileViewModel = viewModel()
) {
    LaunchedEffect(targetUid) { vm.load(targetUid) }

    val profile by vm.profile.collectAsStateWithLifecycle()
    val videos by vm.videos.collectAsStateWithLifecycle()
    val privateVideos by vm.privateVideos.collectAsStateWithLifecycle()
    val reposts by vm.reposts.collectAsStateWithLifecycle()
    val liked by vm.liked.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val isOwn by vm.isOwnProfile.collectAsStateWithLifecycle()
    val isFollowing by vm.isFollowing.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(ProfileTab.VIDEOS) }
    var showMenu by remember { mutableStateOf(false) }
    var showActionsFor by remember { mutableStateOf<Video?>(null) }

    Scaffold(
        containerColor = EarnyBlack,
        topBar = {
            ProfileTopBar(
                username = profile?.username ?: "",
                onMenu = { showMenu = true }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            if (loading && profile == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Gold)
                }
                return@Column
            }

            val p = profile ?: UserProfile()

            ProfileHeader(
                profile = p,
                isOwn = isOwn,
                isFollowing = isFollowing,
                onEdit = { /* TODO */ },
                onShare = { /* TODO */ },
                onFollow = { vm.toggleFollow() }
            )

            ProfileTabs(
                current = currentTab,
                isOwn = isOwn,
                onSelect = { currentTab = it }
            )

            val list = when (currentTab) {
                ProfileTab.VIDEOS -> videos
                ProfileTab.PRIVATE -> if (isOwn) privateVideos else emptyList()
                ProfileTab.REPOSTS -> reposts
                ProfileTab.LIKED -> liked
            }

            when {
                list.isEmpty() -> EmptyTab(tab = currentTab, isOwn = isOwn)
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(list) { v ->
                        VideoTile(
                            video = v,
                            onLongPress = { showActionsFor = v }
                        )
                    }
                }
            }
        }
    }

    if (showMenu) {
        ProfileMenuSheet(
            onDismiss = { showMenu = false },
            onSignOut = {
                showMenu = false
                onSignOut()
            }
        )
    }

    showActionsFor?.let { video ->
        VideoActionsSheet(
            video = video,
            isOwn = isOwn,
            onDismiss = { showActionsFor = null },
            onPin = { vm.togglePin(video); showActionsFor = null },
            onPrivacy = { vm.togglePrivacy(video); showActionsFor = null },
            onDelete = { vm.deleteVideo(video); showActionsFor = null }
        )
    }
}

/* ═══════════════════ TOP BAR ═══════════════════ */

@Composable
private fun ProfileTopBar(username: String, onMenu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (username.isBlank()) "@username" else "@$username",
                color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.KeyboardArrowDown, null,
                tint = TextWhite, modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Filled.PersonAdd, "add",
            tint = TextWhite,
            modifier = Modifier.size(26.dp).padding(end = 4.dp)
        )
        Spacer(Modifier.width(14.dp))
        Icon(
            Icons.Filled.Menu, "menu", tint = TextWhite,
            modifier = Modifier.size(26.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onMenu
            )
        )
    }
}

/* ═══════════════════ HEADER ═══════════════════ */

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
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        // Avatar centered, big
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(contentAlignment = Alignment.BottomEnd) {
                // Gold story ring
                Box(
                    Modifier.size(100.dp)
                        .background(
                            Brush.sweepGradient(listOf(Gold, Orange, Gold)),
                            CircleShape
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(EarnyInput)
                            .padding(8.dp)
                    )
                }
                // Plus badge
                Box(
                    Modifier.size(26.dp)
                        .background(Gold, CircleShape)
                        .border(3.dp, EarnyBlack, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add, null, tint = EarnyBlack,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Username + verified
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "@" + (profile.username.ifBlank { "username" }),
                color = TextWhite, fontSize = 15.sp,
                fontWeight = FontWeight.Bold, maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

        // Stats row
        Row(Modifier.fillMaxWidth()) {
            StatBox(formatCount(profile.following), "Following", Modifier.weight(1f))
            StatBox(formatCount(profile.followers), "Followers", Modifier.weight(1f))
            StatBox(formatCount(profile.totalLikes), "Likes", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons
        if (isOwn) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallOutlineButton(
                    text = "Edit profile",
                    modifier = Modifier.weight(1f),
                    onClick = onEdit
                )
                SmallOutlineButton(
                    text = "Share profile",
                    modifier = Modifier.weight(1f),
                    onClick = onShare
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    EarnyButton(
                        text = if (isFollowing) "Following" else "Follow",
                        gradient = !isFollowing,
                        onClick = onFollow
                    )
                }
                SmallIconButton(Icons.Filled.Email, Modifier.width(48.dp))
                SmallIconButton(Icons.Filled.Share, Modifier.width(48.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        // Bio
        if (profile.bio.isNotBlank()) {
            BioText(profile.bio)
            Spacer(Modifier.height(6.dp))
        }

        // Link
        if (profile.link.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Link, null,
                    tint = TextWhite, modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    profile.link,
                    color = Color(0xFF6BA6FF), fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
private fun SmallOutlineButton(text: String, modifier: Modifier, onClick: () -> Unit) {
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
private fun SmallIconButton(icon: ImageVector, modifier: Modifier) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EarnySurface)
            .border(1.dp, EarnyBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = TextWhite, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BioText(bio: String) {
    Text(
        text = bio,
        color = TextWhite,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

/* ═══════════════════ TABS ═══════════════════ */

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
        modifier.fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon, null,
                tint = if (selected) TextWhite else TextWhite40,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(2.dp))
            Box(
                Modifier
                    .width(if (selected) 24.dp else 0.dp)
                    .height(2.dp)
                    .background(if (selected) TextWhite else Color.Transparent)
            )
        }
    }
}

/* ═══════════════════ GRID TILE ═══════════════════ */

@Composable
private fun VideoTile(video: Video, onLongPress: () -> Unit) {
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
            .pointerInput(video.id) {
                detectTapGestures(onLongPress = { onLongPress() })
            }
    ) {
        // Play icon center
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.PlayArrow, null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(30.dp)
            )
        }

        // Pin badge top-left
        if (video.isPinned) {
            Row(
                Modifier.align(Alignment.TopStart).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PushPin, null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "Pinned",
                    color = Color.White, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Private badge
        if (video.isPrivate) {
            Icon(
                Icons.Filled.Lock, null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(13.dp)
            )
        }

        // Views count bottom-left
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
                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

/* ═══════════════════ EMPTY TAB ═══════════════════ */

@Composable
private fun EmptyTab(tab: ProfileTab, isOwn: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val (icon, title, sub) = when (tab) {
            ProfileTab.VIDEOS -> Triple(
                Icons.Filled.Videocam,
                "No videos yet",
                if (isOwn) "Tap Earny Orb to post" else "This user hasn't posted"
            )
            ProfileTab.PRIVATE -> Triple(
                Icons.Filled.Lock,
                "No private videos",
                "Only you can see these"
            )
            ProfileTab.REPOSTS -> Triple(
                Icons.Filled.Repeat,
                "No reposts yet",
                "Videos you repost will appear here"
            )
            ProfileTab.LIKED -> Triple(
                Icons.Filled.Favorite,
                "No liked videos",
                "Videos you like appear here"
            )
        }

        Box(
            Modifier.size(72.dp).clip(CircleShape).background(EarnyInput),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = TextWhite60, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(sub, color = TextWhite60, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

/* ═══════════════════ MENU SHEET ═══════════════════ */

@Composable
private fun ProfileMenuSheet(onDismiss: () -> Unit, onSignOut: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = EarnySurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextWhite40) }
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            MenuRow(Icons.Filled.Dashboard, "Creator tools") {}
            MenuRow(Icons.Filled.QrCode, "QR code") {}
            MenuRow(Icons.Filled.Share, "Share profile") {}
            MenuRow(Icons.Filled.Settings, "Settings and privacy") {}
            HorizontalDivider(color = EarnyBorder, thickness = 1.dp)
            MenuRow(
                Icons.Filled.Logout, "Log out",
                tint = Color(0xFFE53935)
            ) { onSignOut() }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    tint: Color = TextWhite,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/* ═══════════════════ VIDEO ACTIONS SHEET ═══════════════════ */

@Composable
private fun VideoActionsSheet(
    video: Video,
    isOwn: Boolean,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onPrivacy: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = EarnySurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextWhite40) }
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            if (isOwn) {
                MenuRow(
                    Icons.Filled.PushPin,
                    if (video.isPinned) "Unpin from profile" else "Pin to profile"
                ) { onPin() }
                MenuRow(
                    if (video.isPrivate) Icons.Filled.Public else Icons.Filled.Lock,
                    if (video.isPrivate) "Make public" else "Make private"
                ) { onPrivacy() }
                HorizontalDivider(color = EarnyBorder, thickness = 1.dp)
                MenuRow(
                    Icons.Filled.Delete, "Delete video",
                    tint = Color(0xFFE53935)
                ) { onDelete() }
            } else {
                MenuRow(Icons.Filled.Bookmark, "Save video") {}
                MenuRow(Icons.Filled.Share, "Share") {}
                MenuRow(Icons.Filled.Report, "Report", tint = Color(0xFFE53935)) {}
            }
        }
    }
}
