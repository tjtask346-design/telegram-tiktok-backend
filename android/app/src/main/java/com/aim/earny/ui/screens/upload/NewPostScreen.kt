@file:OptIn(ExperimentalMaterial3Api::class)

package com.aim.earny.ui.screens.upload

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.aim.earny.data.Draft
import com.aim.earny.data.DraftRepository
import java.io.File
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.rememberCoroutineScope
import com.aim.earny.ui.theme.*
import com.aim.earny.vm.UploadState
import com.aim.earny.vm.UploadViewModel

@Composable
fun NewPostScreen(
    onClose: () -> Unit,
    onPosted: () -> Unit
) {
    val context = LocalContext.current
    val vm: UploadViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(true) }
    var draftMode by remember { mutableStateOf<Draft?>(null) }
    var showDrafts by remember { mutableStateOf(false) }
    var drafts by remember { mutableStateOf<List<Draft>>(emptyList()) }
    var savingDraft by remember { mutableStateOf(false) }

    val draftRepo = remember { DraftRepository() }
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) { selectedUri = uri; showSheet = false }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { selectedUri = it; showSheet = false }
        }
    }

    LaunchedEffect(showDrafts) {
        if (showDrafts) {
            drafts = draftRepo.list()
        }
    }

    // Navigate away when upload done
    LaunchedEffect(state) {
        if (state is UploadState.Done) {
            kotlinx.coroutines.delay(700)
            vm.reset()
            onPosted()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        if (selectedUri == null) {
            // ──── Empty selection state ────
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.size(120.dp).clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Gold.copy(alpha = 0.2f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AddCircle, null,
                        tint = Gold, modifier = Modifier.size(72.dp)
                    )
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    "Create a video",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Record or select from your gallery",
                    color = TextWhite60, fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(36.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BigPickButton(
                        icon = Icons.Filled.CameraAlt,
                        label = "Camera",
                        modifier = Modifier.weight(1f)
                    ) {
                        try {
                            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                                putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
                                putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)
                            }
                            cameraLauncher.launch(intent)
                        } catch (_: Throwable) {
                            galleryLauncher.launch("video/*")
                        }
                    }
                    BigPickButton(
                        icon = Icons.Filled.PhotoLibrary,
                        label = "Gallery",
                        modifier = Modifier.weight(1f)
                    ) { galleryLauncher.launch("video/*") }
                }

                Spacer(Modifier.height(24.dp))

                // Drafts button
                Row(
                    Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(EarnySurface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showDrafts = true
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Drafts, null,
                        tint = Gold, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "View drafts",
                        color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            // Close button top-left
            IconButton(
                onClick = onClose,
                modifier = Modifier.padding(12.dp).align(Alignment.TopStart)
            ) {
                Icon(Icons.Filled.Close, "close", tint = Color.White, modifier = Modifier.size(28.dp))
            }

        } else {
            // ──── Preview + Caption state ────
            val exo = remember {
                ExoPlayer.Builder(context).build().apply {
                    repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    playWhenReady = true
                }
            }
            LaunchedEffect(selectedUri) {
                selectedUri?.let {
                    exo.setMediaItem(MediaItem.fromUri(it))
                    exo.prepare()
                }
            }
            DisposableEffect(Unit) { onDispose { exo.release() } }

            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exo
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay bottom
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(top = 40.dp, start = 12.dp, end = 12.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.ArrowBack, "back", tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.weight(1f))

                // Save draft button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(EarnyInput)
                        .clickable(
                            enabled = !savingDraft,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val uri = selectedUri ?: return@clickable
                            scope.launch {
                                savingDraft = true
                                try {
                                    val d = draftRepo.save(
                                        context, uri, caption, 0, 0, 0
                                    )
                                    // Clear + go back
                                    selectedUri = null
                                    caption = ""
                                    onClose()
                                } catch (_: Exception) {
                                } finally {
                                    savingDraft = false
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (savingDraft) {
                        CircularProgressIndicator(
                            color = Gold, strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            "Draft",
                            color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Post button
                Box(
                    Modifier.clip(RoundedCornerShape(100.dp))
                        .background(Gold)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            vm.upload(context, selectedUri!!, caption, onPosted)
                        }
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Post",
                        color = EarnyBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                }
            }

            // Caption panel at bottom
            Column(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .padding(20.dp).padding(bottom = 32.dp)
            ) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 200) caption = it },
                    placeholder = { Text("Write a caption...", color = TextWhite60) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 120.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Gold.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        cursorColor = Gold
                    )
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Tag, null, tint = Gold, modifier = Modifier.size(14.dp))
                        Text("#earny #fyp", color = Gold, fontSize = 12.sp)
                    }
                    Text("${caption.length}/200", color = TextWhite40, fontSize = 11.sp)
                }
            }
        }

        // ──── Uploading overlay ────
        val s = state
        AnimatedVisibility(
            visible = s is UploadState.Uploading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (s is UploadState.Uploading) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(40.dp)
                    ) {
                        Box(
                            Modifier.size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { s.progress },
                                color = Gold,
                                strokeWidth = 6.dp,
                                modifier = Modifier.size(120.dp)
                            )
                            Text(
                                "${(s.progress * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(28.dp))
                        Text(
                            "Publishing your video…",
                            color = Color.White, fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Uploading your video securely",
                            color = TextWhite60, fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // ──── Error toast-style snackbar ────
        AnimatedVisibility(
            visible = s is UploadState.Error,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
        ) {
            if (s is UploadState.Error) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFB00020))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text(s.msg, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // ──── Drafts bottom sheet ────
    if (showDrafts) {
        ModalBottomSheet(
            onDismissRequest = { showDrafts = false },
            containerColor = EarnySurface,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = TextWhite40)
            }
        ) {
            Column(
                Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Text(
                    "Your drafts",
                    color = Color.White, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                        .padding(20.dp),
                    textAlign = TextAlign.Center
                )

                if (drafts.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Drafts, null,
                            tint = Gold.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No drafts yet",
                            color = Color.White, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Save videos here to post later",
                            color = TextWhite60, fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    ) {
                        items(drafts) { d ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        // Resume: load into post composer
                                        draftMode = d
                                        val f = File(d.localVideoPath)
                                        if (f.exists()) {
                                            selectedUri = Uri.fromFile(f)
                                            caption = d.caption
                                            showDrafts = false
                                            showSheet = false
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                                        .background(EarnyInput),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Videocam, null,
                                        tint = Gold, modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        d.caption.ifBlank { "Untitled" },
                                        color = Color.White, fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        "Tap to resume",
                                        color = TextWhite60, fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    Icons.Filled.Delete, null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(22.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            scope.launch {
                                                draftRepo.delete(context, d)
                                                drafts = draftRepo.list()
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigPickButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(EarnySurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
