package com.aim.earny.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Draft storage:
 *   • Video file: context.filesDir/drafts/{draftId}.mp4  (survives restart)
 *   • Metadata:   users/{uid}/drafts/{draftId}
 */
class DraftRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun draftsDir(ctx: Context): File {
        val dir = File(ctx.filesDir, "drafts")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun save(
        context: Context,
        sourceUri: Uri,
        caption: String,
        duration: Int,
        width: Int,
        height: Int
    ): Draft = withContext(Dispatchers.IO) {
        val me = auth.currentUser?.uid
            ?: throw IllegalStateException("Not signed in")

        val docRef = db.collection("users").document(me)
            .collection("drafts").document()
        val draftId = docRef.id
        val dest = File(draftsDir(context), "$draftId.mp4")

        // Copy video to persistent location
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(dest).use { out -> input.copyTo(out) }
        } ?: throw IllegalStateException("Cannot open video")

        docRef.set(
            mapOf(
                "caption" to caption,
                "localVideoPath" to dest.absolutePath,
                "duration" to duration,
                "width" to width,
                "height" to height,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        Draft(
            id = draftId,
            caption = caption,
            localVideoPath = dest.absolutePath,
            duration = duration,
            width = width,
            height = height,
            createdAtMs = System.currentTimeMillis()
        )
    }

    suspend fun list(): List<Draft> = withContext(Dispatchers.IO) {
        val me = auth.currentUser?.uid ?: return@withContext emptyList()
        runCatching {
            val snap = db.collection("users").document(me)
                .collection("drafts").get().await()
            snap.documents
                .map { DocumentMapper.draft(it) }
                .filter { File(it.localVideoPath).exists() }
                .sortedByDescending { it.createdAtMs }
        }.getOrDefault(emptyList())
    }

    suspend fun delete(context: Context, draft: Draft) = withContext(Dispatchers.IO) {
        val me = auth.currentUser?.uid ?: return@withContext
        runCatching {
            val f = File(draft.localVideoPath)
            if (f.exists()) f.delete()
            db.collection("users").document(me)
                .collection("drafts").document(draft.id)
                .delete().await()
        }.onFailure { Log.w("DraftRepository", "delete failed: $it") }
    }

    /** Called after a successful post so the video file is removed */
    suspend fun deleteLocalFileOnly(draft: Draft) = withContext(Dispatchers.IO) {
        runCatching {
            val f = File(draft.localVideoPath)
            if (f.exists()) f.delete()
        }
    }
}
