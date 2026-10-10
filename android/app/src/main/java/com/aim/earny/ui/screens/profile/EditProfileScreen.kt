package com.aim.earny.ui.screens.profile

import android.content.Intent
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import com.aim.earny.data.ProfilePicRepository
import com.aim.earny.data.ApiService
import com.aim.earny.BuildConfig
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.components.SafeAvatar
import com.aim.earny.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class NameState { Idle, Checking, Available, Taken, Invalid }

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val uid = auth.currentUser?.uid
    val picRepo = remember {
        val api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE.trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        ProfilePicRepository(api)
    }

    val picPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploadingPic = true
        errorMsg = null
        scope.launch {
            try {
                val newId = picRepo.upload(context, uri)
                picMsgId = newId
            } catch (e: Exception) {
                errorMsg = "Pic upload failed: ${e.message}"
            } finally {
                uploadingPic = false
            }
        }
    }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var originalUsername by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var nameState by remember { mutableStateOf(NameState.Idle) }
    var picMsgId by remember { mutableStateOf(0L) }
    var uploadingPic by remember { mutableStateOf(false) }

    // Load existing — defensive
    LaunchedEffect(uid) {
        if (uid == null) { loading = false; return@LaunchedEffect }
        try {
            val doc = db.collection("users").document(uid).get().await()
            firstName = doc.getString("firstName") ?: ""
            lastName = doc.getString("lastName") ?: ""
            username = doc.getString("username") ?: ""
            originalUsername = username.lowercase()
            bio = doc.getString("bio") ?: ""
            link = doc.getString("link") ?: ""
            picMsgId = (doc.get("profilePicMsgId") as? Number)?.toLong() ?: 0L
        } catch (e: Exception) {
            errorMsg = "Couldn't load: ${e.message}"
        }
        loading = false
    }

    // Live username check (debounced)
    LaunchedEffect(username) {
        val clean = username.trim().lowercase()
        if (clean == originalUsername) { nameState = NameState.Idle; return@LaunchedEffect }
        when {
            clean.isEmpty() -> { nameState = NameState.Idle; return@LaunchedEffect }
            clean.length < 3 -> { nameState = NameState.Invalid; return@LaunchedEffect }
            !clean.matches(Regex("^[a-z0-9_.]+$")) -> {
                nameState = NameState.Invalid; return@LaunchedEffect
            }
        }
        nameState = NameState.Checking
        delay(450)
        try {
            val doc = db.collection("usernames").document(clean).get().await()
            nameState = if (!doc.exists()) NameState.Available else NameState.Taken
        } catch (e: Exception) {
            nameState = NameState.Idle
            errorMsg = "Check failed: ${e.message}"
        }
    }

    val canSubmit = firstName.isNotBlank()
            && lastName.isNotBlank()
            && username.length >= 3
            && (username.trim().lowercase() == originalUsername
                || nameState == NameState.Available)
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
            Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile pic row
            Box(contentAlignment = Alignment.BottomEnd) {
                SafeAvatar(
                    name = username.ifBlank { firstName.ifBlank { "?" } },
                    size = 100.dp,
                    picMsgId = picMsgId
                )
                Box(
                    Modifier.size(32.dp)
                        .background(Gold, CircleShape)
                        .clickable(
                            enabled = !uploadingPic,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            picPicker.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uploadingPic) {
                        CircularProgressIndicator(
                            color = EarnyBlack,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.CameraAlt, null,
                            tint = EarnyBlack, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (uploadingPic) "Uploading…" else "Tap camera to change",
                color = TextWhite40, fontSize = 11.sp
            )

            Spacer(Modifier.height(24.dp))

            // First + Last name
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { if (it.length <= 30) firstName = it },
                    label = { Text("First name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { if (it.length <= 30) lastName = it },
                    label = { Text("Last name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors()
                )
            }

            Spacer(Modifier.height(12.dp))

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = {
                    val v = it.lowercase().filter { ch ->
                        ch.isLetterOrDigit() || ch == '_' || ch == '.'
                    }
                    if (v.length <= 20) username = v
                },
                label = { Text("@username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                isError = nameState == NameState.Taken || nameState == NameState.Invalid,
                trailingIcon = {
                    when (nameState) {
                        NameState.Checking -> CircularProgressIndicator(
                            color = Gold, strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        NameState.Available -> Icon(
                            Icons.Filled.Check, null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(22.dp)
                        )
                        NameState.Taken -> Icon(
                            Icons.Filled.Close, null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(22.dp)
                        )
                        else -> {}
                    }
                }
            )

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(start = 6.dp)) {
                val (txt, col) = when (nameState) {
                    NameState.Available -> "✓ @$username is available" to Color(0xFF4CAF50)
                    NameState.Taken -> "✗ @$username is already taken" to Color(0xFFE53935)
                    NameState.Invalid -> "Only 3-20 chars: a-z 0-9 _ ." to Color(0xFFE53935)
                    NameState.Checking -> "Checking…" to TextWhite60
                    NameState.Idle -> "3+ chars, a-z 0-9 _ ." to TextWhite40
                }
                Text(txt, color = col, fontSize = 11.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Bio
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 80) bio = it },
                label = { Text("Bio (max 80)") },
                singleLine = false,
                modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                colors = fieldColors()
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().padding(start = 6.dp)) {
                Text("${bio.length}/80", color = TextWhite40, fontSize = 10.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Link
            OutlinedTextField(
                value = link,
                onValueChange = { if (it.length <= 120) link = it },
                label = { Text("Website link") },
                placeholder = { Text("example.com or https://...", color = TextWhite40) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Link, null,
                        tint = Gold, modifier = Modifier.size(18.dp)
                    )
                }
            )

            if (errorMsg != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    errorMsg!!, color = Color(0xFFE53935),
                    fontSize = 13.sp, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                enabled = canSubmit,
                onClick = {
                    if (uid == null) return@Button
                    saving = true
                    errorMsg = null
                    scope.launch {
                        try {
                            val newClean = username.trim().lowercase()
                            val oldClean = originalUsername

                            if (newClean != oldClean) {
                                db.collection("usernames").document(newClean).set(
                                    mapOf(
                                        "uid" to uid,
                                        "username" to username.trim()
                                    )
                                ).await()
                                if (oldClean.isNotBlank()) {
                                    try {
                                        db.collection("usernames")
                                            .document(oldClean).delete().await()
                                    } catch (_: Exception) {}
                                }
                            }

                            db.collection("users").document(uid).set(
                                mapOf(
                                    "firstName" to firstName.trim(),
                                    "lastName" to lastName.trim(),
                                    "fullName" to "$firstName $lastName".trim(),
                                    "username" to username.trim(),
                                    "usernameLower" to newClean,
                                    "bio" to bio.trim(),
                                    "link" to link.trim()
                                ),
                                SetOptions.merge()
                            ).await()

                            saving = false
                            onSaved()
                        } catch (e: Exception) {
                            saving = false
                            errorMsg = "Save failed: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = EarnyBlack
                )
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        color = EarnyBlack,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Save changes",
                        color = EarnyBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedBorderColor = Gold,
    unfocusedBorderColor = Color(0xFF2A2A2A),
    focusedLabelColor = Gold,
    unfocusedLabelColor = TextWhite40,
    cursorColor = Gold,
    focusedContainerColor = EarnyInput,
    unfocusedContainerColor = EarnyInput
)
