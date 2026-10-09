package com.aim.earny.data

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class UploadRepository(
    private val api: ApiService,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun upload(
        context: Context,
        uri: Uri,
        caption: String,
        duration: Int,
        width: Int,
        height: Int,
        onProgress: (Float) -> Unit
    ): UploadResponse {
        // Copy to cache dir
        val tmp = File(context.cacheDir, "upload_${System.currentTimeMillis()}.mp4")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { out -> input.copyTo(out) }
            } ?: throw IllegalStateException("Cannot open video")

            if (tmp.length() == 0L) throw IllegalStateException("Empty video file")
            if (tmp.length() > 2L * 1024 * 1024 * 1024) {
                throw IllegalStateException("Video must be under 2 GB")
            }

            val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
            val token = user.getIdToken(false).await().token
                ?: throw IllegalStateException("No auth token")

            val progressBody = ProgressRequestBody(
                tmp,
                "video/mp4".toMediaType(),
                onProgress
            )
            val filePart = MultipartBody.Part.createFormData("file", tmp.name, progressBody)
            val text = "text/plain".toMediaType()

            return api.upload(
                "Bearer $token",
                filePart,
                caption.toRequestBody(text),
                duration.toString().toRequestBody(text),
                width.toString().toRequestBody(text),
                height.toString().toRequestBody(text)
            )
        } finally {
            runCatching { tmp.delete() }
        }
    }
}
