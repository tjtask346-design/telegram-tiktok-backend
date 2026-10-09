package com.aim.earny.data

data class Video(
    val id: String = "",
    val uploader: String = "",
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
