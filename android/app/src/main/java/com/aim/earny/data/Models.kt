package com.aim.earny.data

data class Video(
    val id: String = "",
    val uploader: String = "",
    val uploaderName: String = "",
    val uploaderHandle: String = "",
    val caption: String = "",
    val telegramMsgId: Long = 0,
    val likes: Long = 0,
    val views: Long = 0,
    val comments: Long = 0,
    val isPinned: Boolean = false,
    val isPrivate: Boolean = false,
    val isDraft: Boolean = false,
    val isRepost: Boolean = false,
    val createdAtMs: Long = 0L,
    val thumbB64: String = ""
)

data class UploadResponse(
    val videoId: String = "",
    val msgId: Long = 0,
    val size: Long = 0
)

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val bio: String = "",
    val link: String = "",
    val followers: Long = 0,
    val following: Long = 0,
    val totalLikes: Long = 0,
    val videoCount: Long = 0,
    val verified: Boolean = false
)

data class InboxMessage(
    val id: String = "",
    val fromName: String = "",
    val fromUid: String = "",
    val text: String = "",
    val unread: Boolean = false,
    val timestamp: Long = 0L
)
