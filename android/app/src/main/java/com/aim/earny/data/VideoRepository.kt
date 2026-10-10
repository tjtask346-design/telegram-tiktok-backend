package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Handles per-video like state and counter updates.
 *
 * Likes live in a subcollection: videos/{vid}/likes/{uid}
 *   - Existence of the doc = user has liked
 *   - Atomic counters on the parent video doc
 */
class VideoRepository(
    private val api: ApiService,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Has current user liked this video? */
    suspend fun hasLiked(videoId: String): Boolean {
        val me = auth.currentUser?.uid ?: return false
        return runCatching {
            db.collection("videos").document(videoId)
                .collection("likes").document(me)
                .get().await().exists()
        }.getOrDefault(false)
    }

    /**
     * Toggle like. Atomic batch:
     *   - Create/delete likes/{uid}
     *   - Increment/decrement videos/{vid}.likes counter
     *
     * Returns new state (true = liked).
     */
    suspend fun toggleLike(videoId: String): Boolean {
        val me = auth.currentUser?.uid
            ?: throw IllegalStateException("Not signed in")

        val likeRef = db.collection("videos").document(videoId)
            .collection("likes").document(me)
        val videoRef = db.collection("videos").document(videoId)

        val alreadyLiked = likeRef.get().await().exists()
        val newState = !alreadyLiked

        val batch = db.batch()
        if (newState) {
            batch.set(
                likeRef,
                mapOf(
                    "uid" to me,
                    "likedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(videoRef, "likes", FieldValue.increment(1))
        } else {
            batch.delete(likeRef)
            batch.update(videoRef, "likes", FieldValue.increment(-1))
        }
        batch.commit().await()
        return newState
    }

    /**
     * Increment the view counter. Fire-and-forget (best effort).
     * Silently ignores failures — views aren't critical.
     */
    suspend fun incrementView(videoId: String) {
        runCatching {
            val token = auth.currentUser?.getIdToken(false)?.await()?.token
                ?: return
            api.incrementView("Bearer $token", videoId)
        }
    }
}
