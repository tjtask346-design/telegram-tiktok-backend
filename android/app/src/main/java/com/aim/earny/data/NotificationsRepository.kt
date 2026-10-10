package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NotificationsRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun load(): List<AppNotification> {
        val me = auth.currentUser?.uid ?: return emptyList()
        return runCatching {
            val snap = db.collection("users").document(me)
                .collection("notifications")
                .limit(100)
                .get().await()
            snap.documents
                .map { DocumentMapper.notification(it) }
                .sortedByDescending { it.createdAtMs }
        }.getOrDefault(emptyList())
    }

    suspend fun markAllRead() {
        val me = auth.currentUser?.uid ?: return
        runCatching {
            val snap = db.collection("users").document(me)
                .collection("notifications")
                .whereEqualTo("unread", true)
                .get().await()
            val batch = db.batch()
            snap.documents.forEach { d ->
                batch.update(d.reference, "unread", false)
            }
            batch.commit().await()
        }
    }

    suspend fun deleteNotification(id: String) {
        val me = auth.currentUser?.uid ?: return
        runCatching {
            db.collection("users").document(me)
                .collection("notifications").document(id)
                .delete().await()
        }
    }

    suspend fun clearAll() {
        val me = auth.currentUser?.uid ?: return
        runCatching {
            val snap = db.collection("users").document(me)
                .collection("notifications")
                .get().await()
            val batch = db.batch()
            snap.documents.forEach { d -> batch.delete(d.reference) }
            batch.commit().await()
        }
    }
}
