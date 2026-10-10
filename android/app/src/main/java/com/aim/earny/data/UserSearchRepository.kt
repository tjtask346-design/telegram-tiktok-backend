package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserSearchRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Search users by prefix on `usernameLower`.
     * Firestore has no full-text search, so we use range queries:
     *   >= query && < query+'\uf8ff'
     */
    suspend fun search(query: String): List<UserProfile> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        val me = auth.currentUser?.uid ?: ""

        val snap = db.collection("users")
            .orderBy("usernameLower")
            .startAt(q)
            .endAt(q + "\uf8ff")
            .limit(30)
            .get()
            .await()

        return snap.documents
            .map { DocumentMapper.user(it) }
            .filter { it.uid != me }
    }
}
