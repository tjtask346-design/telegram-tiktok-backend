package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val currentUser get() = auth.currentUser

    suspend fun signUp(email: String, password: String) {
        if (password.length < 6) {
            throw IllegalArgumentException("Password must be at least 6 characters")
        }
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("Signup failed")

        val defaultName = email.substringBefore("@").trim().ifBlank { "user" }
        runCatching {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(defaultName)
                    .build()
            ).await()
        }

        runCatching {
            db.collection("users").document(user.uid).set(
                mapOf(
                    "uid" to user.uid,
                    "email" to email.trim(),
                    "name" to defaultName,
                    "emailVerified" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }

        user.sendEmailVerification().await()
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun resendVerification() {
        val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
        user.sendEmailVerification().await()
    }

    suspend fun checkVerified(): Boolean {
        val user = auth.currentUser ?: return false
        user.reload().await()
        val verified = auth.currentUser?.isEmailVerified == true
        if (verified) {
            runCatching {
                db.collection("users").document(user.uid)
                    .update("emailVerified", true).await()
            }
        }
        return verified
    }

    fun signOut() = auth.signOut()

    fun friendlyError(e: Exception): String {
        val msg = e.message ?: return "Unknown error"
        return when {
            msg.contains("already in use", true) -> "This email is already registered"
            msg.contains("password is invalid", true) -> "Wrong password"
            msg.contains("no user record", true) -> "No account with this email"
            msg.contains("badly formatted", true) -> "Invalid email format"
            msg.contains("network", true) -> "Network error — check internet"
            msg.contains("at least 6", true) -> "Password must be at least 6 characters"
            msg.contains("too many requests", true) -> "Too many attempts. Try again in a minute."
            else -> msg
        }
    }
}
