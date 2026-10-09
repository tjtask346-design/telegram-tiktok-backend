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

    suspend fun sendMagicLink(email: String, landingUrl: String) {
        val trimmed = email.trim()
        val result = auth.sendSignInLinkToEmail(trimmed, magicSettings(landingUrl)).await()
        // No-op, actual send happens async
    }

    suspend fun signInWithEmailLink(email: String, link: String) {
        if (!auth.isSignInWithEmailLink(link)) throw IllegalStateException("Invalid link")
        auth.signInWithEmailLink(email.trim(), link).await()
        saveUserDoc(email.trim())
    }

    suspend fun signUpWithPassword(email: String, password: String) {
        if (password.length < 6) throw IllegalArgumentException("Password must be 6+ chars")
        val r = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = r.user ?: throw IllegalStateException("Signup failed")
        user.sendEmailVerification().await()
        saveUserDoc(email.trim())
    }

    suspend fun signInWithPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun checkVerified(): Boolean {
        val u = auth.currentUser ?: return false
        u.reload().await()
        return auth.currentUser?.isEmailVerified == true
    }

    suspend fun updateProfile(username: String, bio: String) {
        val u = auth.currentUser ?: throw IllegalStateException("Not signed in")
        u.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(username).build()).await()
        db.collection("users").document(u.uid).set(
            mapOf(
                "uid" to u.uid,
                "username" to username,
                "bio" to bio,
                "email" to (u.email ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    fun signOut() = auth.signOut()

    private fun magicSettings(url: String) =
        com.google.firebase.auth.ActionCodeSettings.newBuilder()
            .setUrl(url)
            .setHandleCodeInApp(true)
            .build()

    private suspend fun saveUserDoc(email: String) {
        val u = auth.currentUser ?: return
        val handle = email.substringBefore("@").ifBlank { "user" }
        db.collection("users").document(u.uid).set(
            mapOf(
                "uid" to u.uid,
                "email" to email,
                "username" to handle,
                "bio" to "",
                "createdAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    fun friendlyError(e: Exception): String {
        val m = e.message ?: return "Something went wrong"
        return when {
            m.contains("already in use", true) -> "This email is already registered"
            m.contains("password is invalid", true) -> "Wrong password"
            m.contains("no user record", true) -> "No account with this email"
            m.contains("badly formatted", true) -> "Invalid email format"
            m.contains("network", true) -> "Network error — check internet"
            m.contains("too many requests", true) -> "Too many attempts. Wait a minute."
            else -> m
        }
    }
}
