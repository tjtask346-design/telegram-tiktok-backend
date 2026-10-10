@file:OptIn(ExperimentalMaterial3Api::class)

package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.data.UserProfile
import com.aim.earny.data.UserSearchRepository
import com.aim.earny.data.formatCount
import com.aim.earny.ui.components.SafeAvatar
import com.aim.earny.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UserSearchScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val repo = remember { UserSearchRepository() }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }

    // Debounced search
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        touched = true
        searching = true
        delay(400)
        runCatching {
            results = repo.search(q)
        }
        searching = false
    }

    Column(Modifier.fillMaxSize().background(EarnyBlack)) {

        // Top bar with back + search field
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
                onValueChange = { if (it.length <= 30) query = it },
                placeholder = { Text("Search users…", color = TextWhite40) },
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

        when {
            searching -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Gold) }

            query.trim().length < 2 -> Hint("Type 2+ characters to search")

            results.isEmpty() && touched -> Hint("No users found for \"${query.trim()}\"")

            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(results) { user ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onOpenProfile(user.uid) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SafeAvatar(
                            name = user.username.ifBlank {
                                user.fullName.ifBlank { user.email }
                            },
                            size = 52.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "@" + user.username.ifBlank {
                                    user.fullName.ifBlank { "user" }
                                },
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${formatCount(user.followers)} followers",
                                color = TextWhite60,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
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
                text,
                color = TextWhite60,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
