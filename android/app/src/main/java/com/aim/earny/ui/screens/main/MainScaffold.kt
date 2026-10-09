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
import com.aim.earny.ui.components.EarnyOrb
import com.aim.earny.ui.components.UploadSheet
import com.aim.earny.ui.theme.*

@Composable
fun MainScaffold(onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    var showUpload by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(EarnyBlack)) {
        NavHost(
            nav, startDestination = "feed",
            modifier = Modifier.padding(bottom = 80.dp)
        ) {
            composable("feed") { FeedScreen(onSignOut = onSignOut) }
            composable("discover") { DiscoverScreen() }
            composable("inbox") { InboxScreen() }
            composable("profile") { ProfileScreen(onSignOut = onSignOut) }
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
                NavItem(Icons.Filled.Home, "Home", current == "feed", Modifier.weight(1f)) {
                    nav.navigate("feed") { launchSingleTop = true; popUpTo("feed") }
                }
                NavItem(Icons.Filled.People, "Friends", current == "discover", Modifier.weight(1f)) {
                    nav.navigate("discover") { launchSingleTop = true; popUpTo("feed") }
                }
                Box(Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                    EarnyOrb(size = 52.dp) { showUpload = true }
                }
                NavItem(Icons.Filled.Email, "Inbox", current == "inbox", Modifier.weight(1f), badge = true) {
                    nav.navigate("inbox") { launchSingleTop = true; popUpTo("feed") }
                }
                NavItem(Icons.Filled.Person, "You", current == "profile", Modifier.weight(1f)) {
                    nav.navigate("profile") { launchSingleTop = true; popUpTo("feed") }
                }
            }
        }
    }

    if (showUpload) {
        UploadSheet(
            onDismiss = { showUpload = false },
            onUploadStart = { showUpload = false }
        )
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
