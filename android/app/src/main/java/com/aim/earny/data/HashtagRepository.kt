package com.aim.earny.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class TrendingTag(val tag: String, val videoCount: Long)

class HashtagRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Videos that contain the given hashtag (case-insensitive tag). */
    suspend fun videosForTag(tag: String, limit: Long = 30): List<Video> {
        val clean = tag.trim().lowercase().removePrefix("#")
        if (clean.isBlank()) return emptyList()
        return runCatching {
            val snap = db.collection("videos")
                .whereArrayContains("hashtags", clean)
                .limit(limit)
                .get().await()
            snap.documents
                .map { DocumentMapper.video(it) }
                .sortedByDescending { it.createdAtMs }
        }.getOrDefault(emptyList())
    }

    /** Top hashtags by video count. */
    suspend fun trending(limit: Long = 20): List<TrendingTag> {
        return runCatching {
            val snap = db.collection("hashtags")
                .orderBy("videoCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
            snap.documents.map {
                TrendingTag(
                    tag = it.getString("tag") ?: it.id,
                    videoCount = (it.get("videoCount") as? Number)?.toLong() ?: 0L
                )
            }
        }.getOrDefault(emptyList())
    }
}
