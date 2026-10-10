package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserSearchRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Search by username. Tries prefix search first (fast),
     * falls back to client-side contains match if prefix returns nothing.
     */
    suspend fun search(query: String): List<UserProfile> {
        val q = query.trim().lowercase().removePrefix("@")
        if (q.length < 2) return emptyList()
        val me = auth.currentUser?.uid ?: ""

        // 1. Try prefix query
        val prefixResult = runCatching {
            val snap = db.collection("users")
                .orderBy("usernameLower")
                .startAt(q)
                .endAt(q + "\uf8ff")
                .limit(30)
                .get()
                .await()
            snap.documents
                .map { DocumentMapper.user(it) }
                .filter { it.uid != me }
        }.getOrDefault(emptyList())

        if (prefixResult.isNotEmpty()) return prefixResult

        // 2. Fallback: client-side contains match
        return runCatching {
            val snap = db.collection("users").limit(300).get().await()
            snap.documents
                .map { DocumentMapper.user(it) }
                .filter { it.uid != me }
                .filter { u ->
                    u.username.lowercase().contains(q) ||
                    u.fullName.lowercase().contains(q) ||
                    u.email.lowercase().contains(q)
                }
                .take(30)
        }.getOrDefault(emptyList())
    }
}
