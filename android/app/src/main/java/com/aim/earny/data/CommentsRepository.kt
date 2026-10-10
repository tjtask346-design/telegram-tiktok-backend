package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class CommentsRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Loads latest 100 comments, newest first. */
    suspend fun load(videoId: String): List<Comment> {
        val me = auth.currentUser?.uid ?: ""
        val snap = db.collection("videos").document(videoId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()
        return snap.documents.map { DocumentMapper.comment(it, me) }
    }

    /**
     * Add a comment. Batch:
     *   - Add comment doc
     *   - Increment parent video's comments counter
     */
    suspend fun add(videoId: String, text: String): Comment {
        val me = auth.currentUser
            ?: throw IllegalStateException("Not signed in")
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("Empty comment")

        // Fetch my username
        val meDoc = db.collection("users").document(me.uid).get().await()
        val username = (meDoc.getString("username")
            ?: meDoc.getString("fullName")
            ?: me.email?.substringBefore("@")
            ?: "user").trim()

        val commentRef = db.collection("videos").document(videoId)
            .collection("comments").document()
        val videoRef = db.collection("videos").document(videoId)

        val batch = db.batch()
        batch.set(
            commentRef,
            mapOf(
                "videoId" to videoId,
                "uid" to me.uid,
                "username" to username,
                "text" to trimmed,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(videoRef, "comments", FieldValue.increment(1))
        batch.commit().await()

        return Comment(
            id = commentRef.id,
            videoId = videoId,
            uid = me.uid,
            username = username,
            text = trimmed,
            createdAtMs = System.currentTimeMillis(),
            isMine = true
        )
    }

    /** Delete own comment + decrement counter. */
    suspend fun delete(videoId: String, commentId: String) {
        val commentRef = db.collection("videos").document(videoId)
            .collection("comments").document(commentId)
        val videoRef = db.collection("videos").document(videoId)

        val batch = db.batch()
        batch.delete(commentRef)
        batch.update(videoRef, "comments", FieldValue.increment(-1))
        batch.commit().await()
    }
}
