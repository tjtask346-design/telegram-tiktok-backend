package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import com.aim.earny.data.AppEvents
import com.aim.earny.ui.components.EarnyOrb
import com.aim.earny.ui.screens.profile.AddFriendsScreen
import com.aim.earny.ui.screens.profile.EditProfileScreen
import com.aim.earny.ui.screens.upload.NewPostScreen
import com.aim.earny.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MainScaffold(onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route

    var showNewPost by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showAddFriends by remember { mutableStateOf(false) }
    var showOtherProfile by remember { mutableStateOf<String?>(null) }
    var showSearch by remember { mutableStateOf(false) }

    // Reactive unread dot
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var hasUnread by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        db.collection("messages")
            .whereEqualTo("receiver", uid)
            .addSnapshotListener { snap, _ ->
                hasUnread = snap?.documents?.any {
                    it.getBoolean("unread") == true
                } ?: false
            }
    }

    Box(Modifier.fillMaxSize().background(EarnyBlack)) {
        NavHost(
            nav, startDestination = "feed",
            modifier = Modifier.padding(bottom = 80.dp)
        ) {
            composable("feed") {
                FeedScreen(
                    onOpenProfile = { uid -> showOtherProfile = uid },
                    onOpenSearch = { showSearch = true }
                )
            }
            composable("discover") { DiscoverScreen(onOpenProfile = { uid -> showOtherProfile = uid }) }
            composable("inbox") { InboxScreen() }
            composable("profile") {
                ProfileScreen(
                    onSignOut = onSignOut,
                    onEditProfile = { showEditProfile = true },
                    onAddFriends = { showAddFriends = true }
                )
            }
        }

        // Bottom nav
        Box(
            Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomCenter)
                .background(EarnyBlack)
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(Icons.Filled.Home, "Home", current == "feed",
                    Modifier.weight(1f), badge = false) {
                    nav.navigate("feed") { launchSingleTop = true; popUpTo("feed") }
                }
                NavItem(Icons.Filled.People, "Friends", current == "discover",
                    Modifier.weight(1f), badge = false) {
                    nav.navigate("discover") { launchSingleTop = true; popUpTo("feed") }
                }
                Box(Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                    EarnyOrb(size = 52.dp) { showNewPost = true }
                }
                NavItem(Icons.Filled.Email, "Inbox", current == "inbox",
                    Modifier.weight(1f), badge = hasUnread) {
                    nav.navigate("inbox") { launchSingleTop = true; popUpTo("feed") }
                }
                NavItem(Icons.Filled.Person, "You", current == "profile",
                    Modifier.weight(1f), badge = false) {
                    nav.navigate("profile") { launchSingleTop = true; popUpTo("feed") }
                }
            }
        }

        // Overlays — full screen, no nav route needed
        if (showNewPost) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                NewPostScreen(
                    onClose = { showNewPost = false },
                    onPosted = {
                        showNewPost = false
                        // Signal Feed to reload from Firestore
                        AppEvents.triggerFeedRefresh()
                        // Also refresh Profile so the new video appears there
                        AppEvents.triggerProfileRefresh()
                        nav.navigate("feed") { launchSingleTop = true; popUpTo("feed") }
                    }
                )
            }
        }

        if (showEditProfile) {
            Box(Modifier.fillMaxSize().background(EarnyBlack)) {
                EditProfileScreen(
                    onBack = { showEditProfile = false },
                    onSaved = {
                        showEditProfile = false
                        // Signal Profile to reload from Firestore
                        AppEvents.triggerProfileRefresh()
                    }
                )
            }
        }

        if (showAddFriends) {
            Box(Modifier.fillMaxSize().background(EarnyBlack)) {
                AddFriendsScreen(onBack = { showAddFriends = false })
            }
        }

        if (showSearch) {
            Box(Modifier.fillMaxSize().background(EarnyBlack)) {
                UserSearchScreen(
                    onBack = { showSearch = false },
                    onOpenProfile = { uid ->
                        showSearch = false
                        showOtherProfile = uid
                    }
                )
            }
        }

        showOtherProfile?.let { targetUid ->
            Box(Modifier.fillMaxSize().background(EarnyBlack)) {
                ProfileScreen(
                    onSignOut = { showOtherProfile = null },
                    onEditProfile = { /* not own profile */ },
                    onAddFriends = { /* not own profile */ },
                    targetUid = targetUid
                )
            }
            // Simple back handler
            Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { showOtherProfile = null }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
    onClick: () -> Unit
) {
    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Icon(
                    icon, label,
                    tint = if (selected) Color.White else TextWhite40,
                    modifier = Modifier.size(26.dp)
                )
                if (badge) {
                    Box(
                        Modifier.size(8.dp)
                            .background(HeartRed, androidx.compose.foundation.shape.CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = if (selected) Color.White else TextWhite40,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
        )
    }
}
