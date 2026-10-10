package com.aim.earny.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aim.earny.ui.components.EarnyButton
import com.aim.earny.ui.components.EarnyTextField
import com.aim.earny.ui.components.SafeAvatar
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.AuthViewModel
import com.aim.earny.vm.UsernameState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var originalUsername by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val unameState by vm.usernameState.collectAsState()

    // Load existing profile
    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        runCatching {
            val doc = db.collection("users").document(uid).get().await()
            firstName = doc.getString("firstName") ?: ""
            lastName = doc.getString("lastName") ?: ""
            username = doc.getString("username") ?: ""
            originalUsername = username.lowercase()
            bio = doc.getString("bio") ?: ""
            link = doc.getString("link") ?: ""
        }
        loading = false
        // Auto-check current username availability if it changed
        if (username.isNotEmpty()) vm.checkUsername(username)
    }

    val usernameChanged = username.trim().lowercase() != originalUsername
    val canSubmit = firstName.isNotBlank()
            && lastName.isNotBlank()
            && username.length >= 3
            && (!usernameChanged || unameState == UsernameState.Available)
            && !saving

    Column(Modifier.fillMaxSize().background(EarnyBlack)) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
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
            Spacer(Modifier.width(16.dp))
            Text(
                "Edit profile",
                color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gold)
            }
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SafeAvatar(
                name = username.ifBlank { firstName.ifBlank { "?" } },
                size = 100.dp
            )

            Spacer(Modifier.height(24.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    EarnyTextField(
                        value = firstName,
                        onValueChange = { if (it.length <= 30) firstName = it },
                        placeholder = "First name"
                    )
                }
                Box(Modifier.weight(1f)) {
                    EarnyTextField(
                        value = lastName,
                        onValueChange = { if (it.length <= 30) lastName = it },
                        placeholder = "Last name"
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Username with live check
            EarnyTextField(
                value = username,
                onValueChange = {
                    val v = it.lowercase().filter { ch ->
                        ch.isLetterOrDigit() || ch == '_' || ch == '.'
                    }
                    if (v.length <= 20) {
                        username = v
                        vm.checkUsername(v)
                    }
                },
                placeholder = "@username",
                isError = usernameChanged &&
                        (unameState == UsernameState.Taken || unameState == UsernameState.Invalid),
                trailingIcon = {
                    when (unameState) {
                        UsernameState.Checking -> CircularProgressIndicator(
                            color = Gold, strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        UsernameState.Available -> Icon(
                            Icons.Filled.Check, null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(22.dp)
                        )
                        else -> {}
                    }
                }
            )

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(start = 6.dp)) {
                val (txt, col) = when {
                    usernameChanged && unameState == UsernameState.Available ->
                        "✓ @$username is available" to Color(0xFF4CAF50)
                    usernameChanged && unameState == UsernameState.Taken ->
                        "✗ @$username is already taken" to Color(0xFFE53935)
                    usernameChanged && unameState == UsernameState.Invalid ->
                        "Only 3-20 chars: a-z 0-9 _ ." to Color(0xFFE53935)
                    usernameChanged && unameState == UsernameState.Checking ->
                        "Checking…" to TextWhite60
                    else -> "3+ chars, a-z 0-9 _ ." to TextWhite40
                }
                Text(txt, color = col, fontSize = 11.sp)
            }

            Spacer(Modifier.height(12.dp))

            EarnyTextField(
                value = bio,
                onValueChange = { if (it.length <= 80) bio = it },
                placeholder = "Bio (max 80)"
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().padding(start = 6.dp)) {
                Text("${bio.length}/80", color = TextWhite40, fontSize = 10.sp)
            }

            Spacer(Modifier.height(12.dp))

            EarnyTextField(
                value = link,
                onValueChange = { if (it.length <= 100) link = it },
                placeholder = "Website link (optional)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            if (errorMsg != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    errorMsg!!, color = Color(0xFFE53935),
                    fontSize = 13.sp, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))

            EarnyButton(
                text = "Save changes",
                enabled = canSubmit,
                loading = saving,
                gradient = true,
                onClick = {
                    if (uid == null) return@EarnyButton
                    saving = true
                    errorMsg = null

                    // Save to Firestore
                    val clean = username.trim().lowercase()
                    val update = hashMapOf<String, Any>(
                        "firstName" to firstName.trim(),
                        "lastName" to lastName.trim(),
                        "fullName" to "$firstName $lastName".trim(),
                        "username" to username.trim(),
                        "usernameLower" to clean,
                        "bio" to bio.trim(),
                        "link" to link.trim()
                    )

                    db.collection("users").document(uid)
                        .set(update, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            saving = false
                            onSaved()
                        }
                        .addOnFailureListener { e ->
                            saving = false
                            errorMsg = e.message ?: "Save failed"
                        }
                }
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}
