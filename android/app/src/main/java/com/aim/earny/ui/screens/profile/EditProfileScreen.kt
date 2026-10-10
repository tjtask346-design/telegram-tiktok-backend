package com.aim.earny.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.BuildConfig
import com.aim.earny.data.ApiService
import com.aim.earny.data.ProfilePicRepository
import com.aim.earny.ui.components.SafeAvatar
import com.aim.earny.ui.theme.EarnyBlack
import com.aim.earny.ui.theme.EarnyBorder
import com.aim.earny.ui.theme.EarnyInput
import com.aim.earny.ui.theme.Gold
import com.aim.earny.ui.theme.TextWhite
import com.aim.earny.ui.theme.TextWhite40
import com.aim.earny.ui.theme.TextWhite60
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private enum class NameState { Idle, Checking, Available, Taken, Invalid }

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uid = auth.currentUser?.uid

    val picRepo = remember {
        val api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE.trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        ProfilePicRepository(api)
    }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var originalUsername by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var picMsgId by remember { mutableStateOf(0L) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var uploadingPic by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var nameState by remember { mutableStateOf(NameState.Idle) }

    // Pic picker
    val picPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
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
    }

    LaunchedEffect(uid) {
        if (uid == null) {
            loading = false
        } else {
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
                errorMsg = "Couldn't load profile"
            }
            loading = false
        }
    }

    LaunchedEffect(username) {
        val clean = username.trim().lowercase()
        if (clean == originalUsername) {
            nameState = NameState.Idle
        } else if (clean.isEmpty()) {
            nameState = NameState.Idle
        } else if (clean.length < 3) {
            nameState = NameState.Invalid
        } else if (!clean.matches(Regex("^[a-z0-9_.]+$"))) {
            nameState = NameState.Invalid
        } else {
            nameState = NameState.Checking
            delay(450)
            try {
                val doc = db.collection("usernames").document(clean).get().await()
                nameState = if (doc.exists()) NameState.Taken else NameState.Available
            } catch (e: Exception) {
                nameState = NameState.Idle
            }
        }
    }

    val canSubmit = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            username.length >= 3 &&
            (username.trim().lowercase() == originalUsername || nameState == NameState.Available) &&
            !saving

    Column(modifier = Modifier.fillMaxSize().background(EarnyBlack)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "back",
                tint = TextWhite,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    )
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Edit profile",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gold)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with pic picker
                Box(contentAlignment = Alignment.BottomEnd) {
                    SafeAvatar(
                        name = username.ifBlank { firstName.ifBlank { "?" } },
                        size = 100.dp,
                        picMsgId = picMsgId
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
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
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "change pic",
                                tint = EarnyBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (uploadingPic) "Uploading…" else "Tap camera to change photo",
                    color = TextWhite40,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                color = Gold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            NameState.Available -> Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(22.dp)
                            )
                            NameState.Taken -> Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(22.dp)
                            )
                            else -> Box(Modifier.size(0.dp))
                        }
                    }
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val txt: String
                    val col: Color
                    when (nameState) {
                        NameState.Available -> {
                            txt = "✓ @$username is available"
                            col = Color(0xFF4CAF50)
                        }
                        NameState.Taken -> {
                            txt = "✗ @$username is already taken"
                            col = Color(0xFFE53935)
                        }
                        NameState.Invalid -> {
                            txt = "Only 3-20 chars: a-z 0-9 _ ."
                            col = Color(0xFFE53935)
                        }
                        NameState.Checking -> {
                            txt = "Checking…"
                            col = TextWhite60
                        }
                        NameState.Idle -> {
                            txt = "3+ chars, a-z 0-9 _ ."
                            col = TextWhite40
                        }
                    }
                    Text(text = txt, color = col, fontSize = 11.sp)
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 80) bio = it },
                    label = { Text("Bio (max 80)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                    colors = fieldColors()
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${bio.length}/80",
                        color = TextWhite40,
                        fontSize = 10.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = link,
                    onValueChange = { if (it.length <= 120) link = it },
                    label = { Text("Website link") },
                    placeholder = { Text("example.com", color = TextWhite40) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                if (errorMsg != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFE53935),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(28.dp))

                Button(
                    enabled = canSubmit,
                    onClick = {
                        if (uid == null) {
                            // nothing
                        } else {
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
                                                    .document(oldClean)
                                                    .delete()
                                                    .await()
                                            } catch (ignore: Exception) {
                                            }
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
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            color = EarnyBlack,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Save changes",
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
