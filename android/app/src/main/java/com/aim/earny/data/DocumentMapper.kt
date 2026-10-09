package com.aim.earny.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot

/**
 * Safe, manual mapping from Firestore documents to Kotlin models.
 * Never uses reflective toObject() — avoids Timestamp/Long/type mismatch
 * crashes across schema evolution.
 */
object DocumentMapper {

    private fun DocumentSnapshot.str(key: String, def: String = ""): String =
        runCatching { getString(key) }.getOrNull() ?: def

    private fun DocumentSnapshot.lng(key: String, def: Long = 0L): Long {
        val v = runCatching { get(key) }.getOrNull() ?: return def
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: def
            else -> def
        }
    }

    private fun DocumentSnapshot.bool(key: String, def: Boolean = false): Boolean {
        val v = runCatching { get(key) }.getOrNull() ?: return def
        return when (v) {
            is Boolean -> v
            is Number -> v.toInt() != 0
            is String -> v.equals("true", true)
            else -> def
        }
    }

    fun video(doc: DocumentSnapshot): Video = Video(
        id = doc.id,
        uploader = doc.str("uploader"),
        uploaderName = doc.str("uploaderName"),
        uploaderHandle = doc.str("uploaderHandle"),
        caption = doc.str("caption"),
        telegramMsgId = doc.lng("telegramMsgId"),
        likes = doc.lng("likes"),
        views = doc.lng("views"),
        comments = doc.lng("comments"),
        isPinned = doc.bool("isPinned"),
        isPrivate = doc.bool("isPrivate"),
        isDraft = doc.bool("isDraft"),
        isRepost = doc.bool("isRepost")
    )

    fun video(doc: QueryDocumentSnapshot): Video = video(doc as DocumentSnapshot)

    fun user(doc: DocumentSnapshot, fallbackUid: String = ""): UserProfile = UserProfile(
        uid = doc.str("uid", fallbackUid).ifBlank { doc.id.ifBlank { fallbackUid } },
        firstName = doc.str("firstName"),
        lastName = doc.str("lastName"),
        fullName = doc.str("fullName"),
        username = doc.str("username"),
        email = doc.str("email"),
        bio = doc.str("bio"),
        link = doc.str("link"),
        followers = doc.lng("followers"),
        following = doc.lng("following"),
        totalLikes = doc.lng("totalLikes"),
        videoCount = doc.lng("videoCount"),
        verified = doc.bool("verified")
    )

    fun user(doc: QueryDocumentSnapshot): UserProfile = user(doc as DocumentSnapshot)

    fun message(doc: DocumentSnapshot): InboxMessage = InboxMessage(
        id = doc.id,
        fromName = doc.str("fromName", "User"),
        fromUid = doc.str("fromUid"),
        text = doc.str("text"),
        unread = doc.bool("unread")
    )

    fun message(doc: QueryDocumentSnapshot): InboxMessage = message(doc as DocumentSnapshot)
}
