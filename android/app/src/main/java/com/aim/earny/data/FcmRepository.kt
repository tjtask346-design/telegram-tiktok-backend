package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FcmRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Call on app start (after login) */
    suspend fun registerToken() {
        val me = auth.currentUser ?: return
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            db.collection("users").document(me.uid)
                .set(
                    mapOf("fcmToken" to token),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
        }
    }
}
