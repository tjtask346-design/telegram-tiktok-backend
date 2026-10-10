@file:OptIn(ExperimentalMaterial3Api::class)

package com.aim.earny.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import com.aim.earny.BuildConfig
import com.aim.earny.data.ApiService
import com.aim.earny.data.AuthRepository
import com.aim.earny.data.BlockRepository
import com.aim.earny.data.DocumentMapper
import com.aim.earny.data.UserProfile
import com.aim.earny.ui.components.SafeAvatar
import com.aim.earny.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onSignOut: () -> Unit
) {
    val nav = rememberNavController()

    NavHost(nav, startDestination = "settings_main") {
        composable("settings_main") {
            SettingsMain(
                onClose = onClose,
                onSignOut = onSignOut,
                onPassword = { nav.navigate("change_password") },
                onBlocked = { nav.navigate("blocked_users") },
                onDelete = { nav.navigate("delete_account") }
            )
        }
        composable("change_password") { ChangePasswordScreen(onBack = { nav.popBackStack() }) }
        composable("blocked_users") { BlockedUsersScreen(onBack = { nav.popBackStack() }) }
        composable("delete_account") {
            DeleteAccountScreen(
                onBack = { nav.popBackStack() },
                onDeleted = { onSignOut(); onClose() }
            )
        }
    }
}

@Composable
private fun SettingsMain(
    onClose: () -> Unit,
    onSignOut: () -> Unit,
    onPassword: () -> Unit,
    onBlocked: () -> Unit,
    onDelete: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(EarnyBlack)) {
        SettingsTopBar("Settings", onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            SectionLabel("ACCOUNT")
            SettingsRow(Icons.Filled.Lock, "Change password", "Update your login password", onClick = onPassword)
            SettingsRow(Icons.Filled.Block, "Blocked users", "Manage who you've blocked", onClick = onBlocked)

            Spacer(Modifier.height(20.dp))
            SectionLabel("DANGER ZONE")
            SettingsRow(
                Icons.Filled.DeleteForever, "Delete account",
                "Permanently remove your account and data",
                danger = true, onClick = onDelete
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("SESSION")
            SettingsRow(Icons.Filled.Logout, "Log out", "Sign out of this device", onClick = onSignOut)

            Spacer(Modifier.height(40.dp))
            Text(
                "Earny v3.5 (build 17)",
                color = TextWhite40, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = TextWhite40, fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp,
        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    sub: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (danger) Color(0xFFE53935) else TextWhite
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EarnySurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = tint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(sub, color = TextWhite60, fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextWhite40, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.ArrowBack, "back",
            tint = TextWhite,
            modifier = Modifier.size(24.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onBack
            )
        )
        Spacer(Modifier.width(16.dp))
        Text(title, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChangePasswordScreen(onBack: () -> Unit) {
    val repo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    var current by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }

    val canSubmit = !loading && current.length >= 6 && newPw.length >= 6 && newPw == confirm

    Column(Modifier.fillMaxSize().background(EarnyBlack)) {
        SettingsTopBar("Change password", onBack)
        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = current,
                onValueChange = { current = it; error = null },
                label = { Text("Current password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = newPw,
                onValueChange = { newPw = it; error = null },
                label = { Text("New password (6+)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it; error = null },
                label = { Text("Confirm new password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                isError = confirm.isNotBlank() && confirm != newPw,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, color = Color(0xFFE53935), fontSize = 13.sp)
            }
            if (ok) {
                Spacer(Modifier.height(12.dp))
                Text("Password updated ✓", color = Color(0xFF4CAF50), fontSize = 13.sp)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                enabled = canSubmit,
                onClick = {
                    scope.launch {
                        loading = true; error = null; ok = false
                        try {
                            repo.changePassword(current, newPw)
                            ok = true
                            current = ""; newPw = ""; confirm = ""
                        } catch (e: Exception) {
                            error = e.message ?: "Failed"
                        } finally { loading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                if (loading) CircularProgressIndicator(
                    color = EarnyBlack, modifier = Modifier.size(22.dp), strokeWidth = 2.dp
                ) else Text("Update password", color = EarnyBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun BlockedUsersScreen(onBack: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    val me = FirebaseAuth.getInstance().currentUser?.uid
    val scope = rememberCoroutineScope()
    val blockRepo = remember { BlockRepository() }

    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() {
        loading = true
        runCatching {
            val blocks = db.collection("blocks").whereEqualTo("blocker", me ?: "").get().await()
            val uids = blocks.documents.mapNotNull { it.getString("blocked") }
            val list = mutableListOf<UserProfile>()
            uids.chunked(10).forEach { chunk ->
                if (chunk.isEmpty()) return@forEach
                val snap = db.collection("users")
                    .whereIn(FieldPath.documentId(), chunk).get().await()
                list += snap.documents.map { DocumentMapper.user(it) }
            }
            users = list
        }
        loading = false
    }

    LaunchedEffect(me) { reload() }

    Column(Modifier.fillMaxSize().background(EarnyBlack)) {
        SettingsTopBar("Blocked users", onBack)

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gold)
            }
        } else if (users.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Block, null, tint = Gold.copy(alpha = 0.6f), modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("No blocked users", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Users you block will appear here", color = TextWhite60, fontSize = 13.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(users) { u ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SafeAvatar(
                            name = u.username.ifBlank { u.fullName.ifBlank { u.email } },
                            size = 48.dp,
                            picMsgId = u.profilePicMsgId
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "@" + u.username.ifBlank { u.fullName.ifBlank { "user" } },
                                color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            Modifier.clip(RoundedCornerShape(100.dp)).background(EarnySurface)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    scope.launch {
                                        runCatching {
                                            blockRepo.toggle(u.uid)
                                            reload()
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Unblock", color = Color(0xFFE53935), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountScreen(onBack: () -> Unit, onDeleted: () -> Unit) {
    val scope = rememberCoroutineScope()
    var typed by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val api = remember {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE.trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    Column(Modifier.fillMaxSize().background(EarnyBlack)) {
        SettingsTopBar("Delete account", onBack)
        Column(Modifier.padding(24.dp)) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE53935).copy(alpha = 0.1f))
                    .padding(16.dp)
            ) {
                Column {
                    Icon(Icons.Filled.Warning, null, tint = Color(0xFFE53935), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "This cannot be undone.",
                        color = Color(0xFFE53935), fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "All your videos, comments, likes, followers, and account data will be permanently deleted.",
                        color = TextWhite60, fontSize = 13.sp, lineHeight = 19.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Type DELETE to confirm:", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it.uppercase().take(10); error = null },
                placeholder = { Text("DELETE", color = TextWhite40) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                isError = typed.isNotBlank() && typed != "DELETE"
            )
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, color = Color(0xFFE53935), fontSize = 13.sp)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                enabled = typed == "DELETE" && !loading,
                onClick = {
                    scope.launch {
                        loading = true; error = null
                        try {
                            AuthRepository().deleteAccountViaBackend(api)
                            onDeleted()
                        } catch (e: Exception) {
                            error = e.message ?: "Delete failed"
                        } finally { loading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                if (loading) CircularProgressIndicator(
                    color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp
                ) else Text("Delete my account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedBorderColor = Gold,
    unfocusedBorderColor = EarnyBorder,
    focusedLabelColor = Gold,
    unfocusedLabelColor = TextWhite40,
    cursorColor = Gold,
    focusedContainerColor = EarnyInput,
    unfocusedContainerColor = EarnyInput
)
