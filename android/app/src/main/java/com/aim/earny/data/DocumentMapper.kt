package com.aim.earny.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot

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

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.listOfStrings(key: String): List<String> {
        val v = runCatching { get(key) }.getOrNull() ?: return emptyList()
        return when (v) {
            is List<*> -> v.mapNotNull { (it as? String)?.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
    }

    private fun DocumentSnapshot.timestampMs(key: String): Long {
        val v = runCatching { get(key) }.getOrNull() ?: return 0L
        return when (v) {
            is Timestamp -> v.seconds * 1000L + (v.nanoseconds / 1_000_000)
            is Number -> v.toLong()
            is java.util.Date -> v.time
            else -> 0L
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
        isRepost = doc.bool("isRepost"),
        createdAtMs = doc.timestampMs("createdAt"),
        thumbB64 = doc.str("thumbB64"),
        hashtags = doc.listOfStrings("hashtags")
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
        verified = doc.bool("verified"),
        profilePicMsgId = doc.lng("profilePicMsgId")
    )

    fun user(doc: QueryDocumentSnapshot): UserProfile = user(doc as DocumentSnapshot)

    fun message(doc: DocumentSnapshot): InboxMessage = InboxMessage(
        id = doc.id,
        fromName = doc.str("fromName", "User"),
        fromUid = doc.str("fromUid"),
        text = doc.str("text"),
        unread = doc.bool("unread"),
        timestamp = doc.timestampMs("timestamp")
    )

    fun message(doc: QueryDocumentSnapshot): InboxMessage = message(doc as DocumentSnapshot)

    fun comment(doc: DocumentSnapshot, meUid: String): Comment = Comment(
        id = doc.id,
        videoId = doc.str("videoId"),
        uid = doc.str("uid"),
        username = doc.str("username"),
        text = doc.str("text"),
        createdAtMs = doc.timestampMs("createdAt"),
        isMine = doc.str("uid") == meUid
    )

    fun notification(doc: DocumentSnapshot): AppNotification = AppNotification(
        id = doc.id,
        kind = doc.str("kind"),
        title = doc.str("title"),
        body = doc.str("body"),
        videoId = doc.str("videoId"),
        senderUid = doc.str("senderUid"),
        senderName = doc.str("senderName"),
        unread = doc.bool("unread"),
        createdAtMs = doc.timestampMs("createdAt")
    )
}
