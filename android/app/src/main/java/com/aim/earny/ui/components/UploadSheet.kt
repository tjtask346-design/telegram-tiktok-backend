package com.aim.earny.ui.components

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadSheet(
    onDismiss: () -> Unit,
    onUploadStart: () -> Unit
) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var uploading by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) selectedUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedUri = result.data?.data
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EarnySurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextWhite40) }
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create Video", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (selectedUri == null) "Choose a source" else "Add a caption",
                color = TextWhite60, fontSize = 13.sp
            )

            Spacer(Modifier.height(28.dp))

            if (selectedUri == null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OptionCard(
                        icon = Icons.Filled.CameraAlt,
                        title = "Camera",
                        subtitle = "Record now",
                        modifier = Modifier.weight(1f)
                    ) {
                        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
                            putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)
                        }
                        try {
                            cameraLauncher.launch(intent)
                        } catch (t: Throwable) {
                            Toast.makeText(ctx, "Camera not available", Toast.LENGTH_SHORT).show()
                        }
                    }
                    OptionCard(
                        icon = Icons.Filled.PhotoLibrary,
                        title = "Gallery",
                        subtitle = "Pick video",
                        modifier = Modifier.weight(1f)
                    ) {
                        galleryLauncher.launch("video/*")
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                            .background(Gold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Upload, null, tint = Gold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Video selected", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Ready to upload", color = TextWhite60, fontSize = 12.sp)
                    }
                    IconButton(onClick = { selectedUri = null }) {
                        Icon(Icons.Filled.Close, null, tint = TextWhite60)
                    }
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 200) caption = it },
                    placeholder = { Text("Write a caption...", color = TextWhite40) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        focusedBorderColor = Gold, unfocusedBorderColor = EarnyBorder,
                        cursorColor = Gold
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(20.dp))

                EarnyButton(
                    text = "Upload to Earny",
                    enabled = !uploading,
                    loading = uploading,
                    gradient = true,
                    onClick = {
                        val uri = selectedUri ?: return@EarnyButton
                        uploading = true
                        val dur = try {
                            val mmr = MediaMetadataRetriever().apply { setDataSource(ctx, uri) }
                            val d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLongOrNull()?.div(1000)?.toInt() ?: 0
                            mmr.release()
                            d
                        } catch (t: Throwable) { 0 }

                        val intent = Intent().apply {
                            putExtra("uri", uri.toString())
                            putExtra("caption", caption)
                            putExtra("duration", dur)
                        }
                        onUploadStart()
                        // Caller launches upload via activity result or shared VM
                        UploadBridge.pendingUri = uri
                        UploadBridge.pendingCaption = caption
                        UploadBridge.pendingDuration = dur
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun OptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier.fillMaxWidth().height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(EarnyInput)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                    .background(Gold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Gold, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = TextWhite60, fontSize = 12.sp)
        }
    }
}

object UploadBridge {
    var pendingUri: Uri? = null
    var pendingCaption: String = ""
    var pendingDuration: Int = 0
    fun clear() { pendingUri = null; pendingCaption = ""; pendingDuration = 0 }
}
