package com.aim.earny.data

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfilePicRepository(
    private val api: ApiService,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun upload(context: Context, uri: Uri): Long {
        val me = auth.currentUser ?: throw IllegalStateException("Not signed in")
        val token = me.getIdToken(false).await().token
            ?: throw IllegalStateException("No token")

        val tmp = File(context.cacheDir, "pic_${System.currentTimeMillis()}.jpg")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { out -> input.copyTo(out) }
            } ?: throw IllegalStateException("Cannot open image")

            if (tmp.length() == 0L) throw IllegalStateException("Empty image")
            if (tmp.length() > 5L * 1024 * 1024) throw IllegalStateException("Image > 5 MB")

            val body = tmp.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("file", tmp.name, body)

            val resp = api.uploadProfilePic("Bearer $token", part)
            return resp.msgId
        } finally {
            runCatching { tmp.delete() }
        }
    }
}
