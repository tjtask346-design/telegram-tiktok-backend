package com.aim.earny.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser get() = auth.currentUser

    suspend fun signUpWithDetails(
        firstName: String, lastName: String,
        email: String, password: String
    ) {
        if (firstName.isBlank()) throw IllegalArgumentException("First name required")
        if (lastName.isBlank()) throw IllegalArgumentException("Last name required")
        if (password.length < 6) throw IllegalArgumentException("Password must be at least 6 characters")

        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("Signup failed")

        val fullName = "$firstName $lastName".trim()
        runCatching {
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(fullName).build()
            ).await()
        }

        runCatching {
            db.collection("users").document(user.uid).set(
                mapOf(
                    "uid" to user.uid,
                    "firstName" to firstName.trim(),
                    "lastName" to lastName.trim(),
                    "fullName" to fullName,
                    "email" to email.trim(),
                    "bio" to "",
                    "followers" to 0,
                    "emailVerified" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        user.sendEmailVerification().await()
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun checkVerified(): Boolean {
        val user = auth.currentUser ?: return false
        user.reload().await()
        val ok = auth.currentUser?.isEmailVerified == true
        if (ok) {
            runCatching {
                db.collection("users").document(user.uid)
                    .update("emailVerified", true).await()
            }
        }
        return ok
    }

    suspend fun resendVerification() {
        val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
        user.sendEmailVerification().await()
    }

    fun signOut() = auth.signOut()

    fun friendlyError(e: Exception): String {
        val m = e.message ?: return "Something went wrong"
        return when {
            m.contains("already in use", true) -> "This email is already registered"
            m.contains("password is invalid", true) -> "Wrong password"
            m.contains("no user record", true) -> "No account with this email"
            m.contains("badly formatted", true) -> "Invalid email format"
            m.contains("network", true) -> "Network error — check internet"
            m.contains("too many requests", true) -> "Too many attempts. Wait a minute."
            m.contains("6 characters", true) -> "Password must be 6+ characters"
            else -> m
        }
    }
}
