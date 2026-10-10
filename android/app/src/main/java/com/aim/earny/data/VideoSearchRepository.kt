package com.aim.earny.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class VideoSearchRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Search videos by:
     *  - "#tag" → hashtag-only query
     *  - plain text → caption substring (client-side)
     *
     * Fetches up to 200 recent videos and filters in-memory.
     */
    suspend fun search(query: String, limit: Int = 60): List<Video> {
        val q = query.trim()
        if (q.length < 2) return emptyList()

        // Hashtag mode
        if (q.startsWith("#")) {
            val tag = q.drop(1).lowercase().trim()
            if (tag.isEmpty()) return emptyList()
            return runCatching {
                val snap = db.collection("videos")
                    .whereArrayContains("hashtags", tag)
                    .limit(limit.toLong())
                    .get().await()
                snap.documents
                    .map { DocumentMapper.video(it) }
                    .sortedByDescending { it.createdAtMs }
            }.getOrDefault(emptyList())
        }

        // Text mode: fetch recent, filter client-side
        return runCatching {
            val snap = db.collection("videos")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(200)
                .get().await()
            val needle = q.lowercase()
            snap.documents
                .map { DocumentMapper.video(it) }
                .filter {
                    it.caption.lowercase().contains(needle) ||
                    it.uploaderHandle.lowercase().contains(needle) ||
                    it.uploaderName.lowercase().contains(needle)
                }
                .take(limit)
        }.getOrDefault(emptyList())
    }
}
