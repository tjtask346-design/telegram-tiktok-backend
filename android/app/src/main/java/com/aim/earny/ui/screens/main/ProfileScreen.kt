package com.aim.earny.ui.screens.main

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    targetUid: String? = null,
    vm: ProfileViewModel = viewModel()
) {
    LaunchedEffect(targetUid) { runCatching { vm.load(targetUid) } }

    val profile by vm.profile.collectAsStateWithLifecycle()
    val videos by vm.videos.collectAsStateWithLifecycle()
    val privateVideos by vm.privateVideos.collectAsStateWithLifecycle()
    val reposts by vm.reposts.collectAsStateWithLifecycle()
    val liked by vm.liked.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val isOwn by vm.isOwnProfile.collectAsStateWithLifecycle()
    val isFollowing by vm.isFollowing.collectAsStateWithLifecycle()

    val ctx = LocalContext.current
    var currentTab by remember { mutableStateOf(ProfileTab.VIDEOS) }

    Column(Modifier.fillMaxSize().background(EarnyBlack)) {

        ProfileTopBar(
            username = profile?.username.orEmpty(),
            onAddFriends = onAddFriends,
            onSignOut = onSignOut
        )

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
                    items(list) { v -> VideoTile(video = v) }
                }
            }
        }
    }
}

@Composable
private fun ProfileTopBar(
    username: String,
    onAddFriends: () -> Unit,
    onSignOut: () -> Unit
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
            Icons.Filled.Logout, "logout",
            tint = TextWhite,
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onSignOut
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
                size = 100.dp
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
private fun VideoTile(video: Video) {
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
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.PlayArrow, null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp)
            )
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
        }
        val title = when (tab) {
            ProfileTab.VIDEOS -> "No videos yet"
            ProfileTab.PRIVATE -> "No private videos"
            ProfileTab.REPOSTS -> "No reposts yet"
            ProfileTab.LIKED -> "No liked videos"
        }
        val sub = when (tab) {
            ProfileTab.VIDEOS -> if (isOwn) "Tap Earny Orb to post" else "This user hasn't posted"
            ProfileTab.PRIVATE -> "Only you can see these"
            ProfileTab.REPOSTS -> "Videos you repost will appear here"
            ProfileTab.LIKED -> "Videos you like appear here"
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
