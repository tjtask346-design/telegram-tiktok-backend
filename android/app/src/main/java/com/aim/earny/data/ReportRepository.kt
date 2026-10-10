package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReportRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** reason examples: "spam", "harassment", "nudity", "violence", "other" */
    suspend fun reportUser(targetUid: String, reason: String, note: String = "") {
        val me = auth.currentUser?.uid
            ?: throw IllegalStateException("Not signed in")
        db.collection("reports").document().set(
            mapOf(
                "kind" to "user",
                "reporter" to me,
                "target" to targetUid,
                "reason" to reason,
                "note" to note,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun reportVideo(videoId: String, uploaderUid: String, reason: String, note: String = "") {
        val me = auth.currentUser?.uid
            ?: throw IllegalStateException("Not signed in")
        db.collection("reports").document().set(
            mapOf(
                "kind" to "video",
                "reporter" to me,
                "target" to videoId,
                "uploader" to uploaderUid,
                "reason" to reason,
                "note" to note,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
