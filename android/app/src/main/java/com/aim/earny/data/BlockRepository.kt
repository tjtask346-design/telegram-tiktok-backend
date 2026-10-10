package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Block edge stored at: blocks/{blockerUid}_{blockedUid}
 */
class BlockRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun pairId(a: String, b: String) = "${a}_${b}"

    suspend fun isBlocked(targetUid: String): Boolean {
        val me = auth.currentUser?.uid ?: return false
        if (targetUid.isBlank() || targetUid == me) return false
        return runCatching {
            db.collection("blocks").document(pairId(me, targetUid))
                .get().await().exists()
        }.getOrDefault(false)
    }

    /** All uids that current user has blocked. */
    suspend fun myBlockedUids(): Set<String> {
        val me = auth.currentUser?.uid ?: return emptySet()
        return runCatching {
            val snap = db.collection("blocks")
                .whereEqualTo("blocker", me)
                .get().await()
            snap.documents.mapNotNull { it.getString("blocked") }.toSet()
        }.getOrDefault(emptySet())
    }

    /** Toggle. Returns new state (true = now blocked). */
    suspend fun toggle(targetUid: String): Boolean {
        val me = auth.currentUser?.uid
            ?: throw IllegalStateException("Not signed in")
        if (targetUid == me) return false

        val ref = db.collection("blocks").document(pairId(me, targetUid))
        val exists = ref.get().await().exists()
        if (exists) {
            ref.delete().await()
            // Best-effort: also remove my follow of them
            runCatching {
                db.collection("follows").document(pairId(me, targetUid)).delete().await()
            }
            return false
        } else {
            ref.set(
                mapOf(
                    "blocker" to me,
                    "blocked" to targetUid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return true
        }
    }
}
