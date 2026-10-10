package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Follow system using a flat `follows` collection.
 *
 * Document ID pattern: "{followerUid}_{followeeUid}"
 *
 * This lets the Firestore rules allow both:
 *   - The follower to create/delete the follow doc
 *   - The follower to increment BOTH users' counters
 */
class FollowRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun pairId(a: String, b: String) = "${a}_${b}"

    /** Returns true if current user follows targetUid */
    suspend fun isFollowing(targetUid: String): Boolean {
        val me = auth.currentUser?.uid ?: return false
        if (me == targetUid) return false
        return runCatching {
            db.collection("follows").document(pairId(me, targetUid))
                .get().await().exists()
        }.getOrDefault(false)
    }

    /**
     * Toggle follow state. Atomic batch write:
     *   1. Add/remove `follows/{me_target}` doc
     *   2. Increment target's followers count
     *   3. Increment my following count
     *
     * Returns the NEW state (true = now following).
     */
    suspend fun toggleFollow(targetUid: String): Boolean {
        val me = auth.currentUser?.uid
            ?: throw IllegalStateException("Not signed in")
        if (me == targetUid) return false

        val followRef = db.collection("follows").document(pairId(me, targetUid))
        val meRef = db.collection("users").document(me)
        val targetRef = db.collection("users").document(targetUid)

        val currentlyFollowing = followRef.get().await().exists()
        val newState = !currentlyFollowing

        val batch = db.batch()
        if (newState) {
            batch.set(
                followRef,
                mapOf(
                    "follower" to me,
                    "followee" to targetUid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(targetRef, "followers", FieldValue.increment(1))
            batch.update(meRef, "following", FieldValue.increment(1))
        } else {
            batch.delete(followRef)
            batch.update(targetRef, "followers", FieldValue.increment(-1))
            batch.update(meRef, "following", FieldValue.increment(-1))
        }
        batch.commit().await()
        return newState
    }

    /** Get current user's following count from their doc */
    suspend fun getMyFollowingCount(): Long {
        val me = auth.currentUser?.uid ?: return 0
        return runCatching {
            val doc = db.collection("users").document(me).get().await()
            (doc.get("following") as? Number)?.toLong() ?: 0L
        }.getOrDefault(0L)
    }
}
