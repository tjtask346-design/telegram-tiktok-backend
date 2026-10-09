package com.aim.earny.data

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class VideoRepository(
    private val api: ApiService,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val auth = FirebaseAuth.getInstance()

    private suspend fun token(): String {
        val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
        val t = user.getIdToken(false).await().token
            ?: throw IllegalStateException("No token")
        return "Bearer $t"
    }

    suspend fun feed(): List<Video> {
        val snap = db.collection("videos")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30).get().await()
        return snap.documents.mapNotNull { d ->
            d.toObject(Video::class.java)?.copy(id = d.id)
        }
    }

    suspend fun upload(
        ctx: Context, uri: Uri, caption: String,
        duration: Int, w: Int, h: Int,
    ): UploadResponse {
        val tmp = File(ctx.cacheDir, "up_${System.currentTimeMillis()}.mp4")
        ctx.contentResolver.openInputStream(uri)!!.use { input ->
            FileOutputStream(tmp).use { out -> input.copyTo(out) }
        }
        try {
            val body = tmp.asRequestBody("video/mp4".toMediaType())
            val part = MultipartBody.Part.createFormData("file", tmp.name, body)
            val textType = "text/plain".toMediaType()
            return api.upload(
                token(), part,
                caption.toRequestBody(textType),
                duration.toString().toRequestBody(textType),
                w.toString().toRequestBody(textType),
                h.toString().toRequestBody(textType),
            )
        } finally {
            tmp.delete()
        }
    }

    suspend fun markViewed(videoId: String) {
        runCatching { api.view(token(), videoId) }
    }

    suspend fun like(videoId: String) {
        db.collection("videos").document(videoId)
            .update("likes", FieldValue.increment(1)).await()
    }
}
