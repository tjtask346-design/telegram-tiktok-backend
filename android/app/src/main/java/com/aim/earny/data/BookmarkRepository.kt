package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Bookmark = private save of a video.
 * Stored in: users/{uid}/bookmarks/{videoId}
 */
class BookmarkRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun hasBookmarked(videoId: String): Boolean {
        val me = auth.currentUser?.uid ?: return false
        return runCatching {
            db.collection("users").document(me)
                .collection("bookmarks").document(videoId)
                .get().await().exists()
        }.getOrDefault(false)
    }

    /** Toggle. Returns new state (true = bookmarked). */
    suspend fun toggle(videoId: String): Boolean {
        val me = auth.currentUser?.uid
            ?: throw IllegalStateException("Not signed in")
        val ref = db.collection("users").document(me)
            .collection("bookmarks").document(videoId)
        val exists = ref.get().await().exists()
        if (exists) {
            ref.delete().await()
            return false
        } else {
            ref.set(
                mapOf(
                    "videoId" to videoId,
                    "savedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return true
        }
    }
}
