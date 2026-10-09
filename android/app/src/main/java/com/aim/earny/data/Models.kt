package com.aim.earny.data

data class Video(
    val id: String = "",
    val uploader: String = "",
    val uploaderUid: String = "",
    val caption: String = "",
    val telegramMsgId: Long = 0,
    val likes: Long = 0,
    val views: Long = 0,
    val comments: Long = 0
)

data class UploadResponse(
    val videoId: String,
    val msgId: Long,
    val size: Long
)

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val bio: String = "",
    val followers: Long = 0
)

data class InboxMessage(
    val id: String = "",
    val fromName: String = "",
    val fromUid: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val unread: Boolean = false
)
