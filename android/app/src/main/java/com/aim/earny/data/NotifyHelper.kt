package com.aim.earny.data

import com.aim.earny.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Fire-and-forget notification trigger.
 * Never blocks UI; ignores all errors.
 */
object NotifyHelper {

    private val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE.trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    suspend fun like(targetUid: String, videoId: String, title: String, body: String) =
        send(targetUid, "like", title, body, videoId)

    suspend fun comment(targetUid: String, videoId: String, title: String, body: String) =
        send(targetUid, "comment", title, body, videoId)

    suspend fun follow(targetUid: String, title: String, body: String) =
        send(targetUid, "follow", title, body, "")

    private suspend fun send(
        targetUid: String,
        kind: String,
        title: String,
        body: String,
        videoId: String
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val me = FirebaseAuth.getInstance().currentUser ?: return@runCatching
            if (me.uid == targetUid) return@runCatching
            val token = me.getIdToken(false).await().token ?: return@runCatching
            api.notify(
                "Bearer $token",
                NotifyRequest(
                    target_uid = targetUid,
                    kind = kind,
                    title = title,
                    body = body,
                    video_id = videoId
                )
            )
        }
    }
}
