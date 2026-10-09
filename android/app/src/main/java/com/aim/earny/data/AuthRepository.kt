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

    /** Returns true if username is free (case-insensitive) */
    suspend fun isUsernameAvailable(username: String): Boolean {
        val clean = username.trim().lowercase()
        if (clean.length < 3) return false
        if (!clean.matches(Regex("^[a-z0-9_.]+$"))) return false
        val snap = db.collection("users")
            .whereEqualTo("usernameLower", clean)
            .limit(1)
            .get()
            .await()
        return snap.isEmpty
    }

    suspend fun signUpWithDetails(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String
    ) {
        if (firstName.isBlank()) throw IllegalArgumentException("First name required")
        if (lastName.isBlank()) throw IllegalArgumentException("Last name required")
        if (password.length < 6) throw IllegalArgumentException("Password must be 6+ characters")

        val clean = username.trim().lowercase()
        if (clean.length < 3) throw IllegalArgumentException("Username must be 3+ characters")
        if (!clean.matches(Regex("^[a-z0-9_.]+$"))) {
            throw IllegalArgumentException("Username: only a-z, 0-9, _ and . allowed")
        }

        // Final uniqueness check
        if (!isUsernameAvailable(clean)) {
            throw IllegalStateException("Username already taken")
        }

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
                    "username" to username.trim(),
                    "usernameLower" to clean,
                    "email" to email.trim(),
                    "bio" to "",
                    "followers" to 0,
                    "videoCount" to 0,
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
            m.contains("username already taken", true) -> "That username is taken"
            m.contains("password is invalid", true) -> "Wrong password"
            m.contains("no user record", true) -> "No account with this email"
            m.contains("badly formatted", true) -> "Invalid email format"
            m.contains("network", true) -> "Network error — check internet"
            m.contains("too many requests", true) -> "Too many attempts. Wait a minute."
            else -> m
        }
    }
}
