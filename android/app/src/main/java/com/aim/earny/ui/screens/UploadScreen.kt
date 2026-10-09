package com.aim.earny.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aim.earny.vm.UploadState
import com.aim.earny.vm.UploadViewModel

@Composable
fun UploadScreen(onDone: () -> Unit, vm: UploadViewModel = viewModel()) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is UploadState.Done) {
            uri = null; caption = ""
            vm.reset()
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri = it }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF0A0E14)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Upload Video", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = { picker.launch("video/*") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(if (uri == null) "Select Video" else "Video Selected", color = Color.White) }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = caption, onValueChange = { caption = it },
            label = { Text("Caption") }, modifier = Modifier.fillMaxWidth(),
            colors = authFieldColors()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            enabled = uri != null && state !is UploadState.Uploading,
            onClick = { uri?.let { vm.upload(ctx, it, caption, onDone) } },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5A0))
        ) {
            Text(
                if (state is UploadState.Uploading) "Uploading..." else "Upload",
                color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        }

        if (state is UploadState.Uploading) {
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(color = Color(0xFF00E5A0))
            Spacer(Modifier.height(8.dp))
            Text("Uploading to Telegram...", color = Color.Gray, fontSize = 12.sp)
        }

        if (state is UploadState.Error) {
            Spacer(Modifier.height(16.dp))
            Text("Error: " + (state as UploadState.Error).msg, color = Color(0xFFFF5C5C), fontSize = 13.sp)
        }
    }
}
