package com.aim.earny.data

import com.aim.earny.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE.trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /** Sorted UID pair — both users see same chat ID. */
    private fun chatIdFor(a: String, b: String): String =
        if (a < b) "${a}_${b}" else "${b}_${a}"

    /** Get or create a chat with another user. Returns chatId. */
    suspend fun ensureChat(otherUid: String): String {
        val me = auth.currentUser?.uid
            ?: throw IllegalStateException("Not signed in")
        if (otherUid.isBlank() || otherUid == me) {
            throw IllegalArgumentException("Invalid chat target")
        }
        val chatId = chatIdFor(me, otherUid)
        val ref = db.collection("chats").document(chatId)
        val exists = ref.get().await().exists()
        if (!exists) {
            ref.set(
                mapOf(
                    "participants" to listOf(me, otherUid),
                    "lastMessage" to "",
                    "lastAt" to FieldValue.serverTimestamp(),
                    "unread_$me" to 0,
                    "unread_$otherUid" to 0,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return chatId
    }

    /** Real-time message stream for a chat. */
    fun messagesStream(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val me = auth.currentUser?.uid ?: ""
        val reg: ListenerRegistration = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(200)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { DocumentMapper.chatMessage(it, me) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Send a message. Updates parent chat + fires DM notification. */
    suspend fun send(chatId: String, receiverUid: String, text: String) {
        val me = auth.currentUser?.uid
            ?: throw IllegalStateException("Not signed in")
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val msgRef = db.collection("chats").document(chatId)
            .collection("messages").document()
        val chatRef = db.collection("chats").document(chatId)

        val batch = db.batch()
        batch.set(
            msgRef,
            mapOf(
                "sender" to me,
                "text" to trimmed,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            chatRef,
            mapOf(
                "lastMessage" to trimmed.take(80),
                "lastAt" to FieldValue.serverTimestamp(),
                "unread_$receiverUid" to FieldValue.increment(1)
            )
        )
        batch.commit().await()

        // Fire notification (fire and forget)
        runCatching {
            val token = me.let { auth.currentUser!!.getIdToken(false).await().token }
                ?: return@runCatching
            api.notifyDm(
                "Bearer $token",
                DmNotifyRequest(
                    chatId = chatId,
                    receiverUid = receiverUid,
                    text = trimmed.take(80)
                )
            )
        }
    }

    /** Mark all unread for me in this chat as read. */
    suspend fun markRead(chatId: String) {
        val me = auth.currentUser?.uid ?: return
        runCatching {
            db.collection("chats").document(chatId)
                .update("unread_$me", 0).await()
        }
    }

    /** List of chats for current user, newest first. */
    suspend fun myChats(): List<Chat> {
        val me = auth.currentUser?.uid ?: return emptyList()
        return runCatching {
            val snap = db.collection("chats")
                .whereArrayContains("participants", me)
                .get().await()

            val list = mutableListOf<Chat>()
            for (d in snap.documents) {
                val parts = (d.get("participants") as? List<*>)
                    ?.mapNotNull { it as? String } ?: continue
                val other = parts.firstOrNull { it != me } ?: continue

                // Fetch other user's profile
                val u = db.collection("users").document(other).get().await()
                val ud = u.toDictionary()
                val uname = (ud["username"] as? String)
                    ?: (ud["fullName"] as? String)
                    ?: "user"
                val pic = (ud["profilePicMsgId"] as? Number)?.toLong() ?: 0L

                val lastAt = when (val v = d.get("lastAt")) {
                    is com.google.firebase.Timestamp -> v.seconds * 1000L
                    is Number -> v.toLong()
                    else -> 0L
                }
                val unread = (d.get("unread_$me") as? Number)?.toLong() ?: 0L

                list += Chat(
                    id = d.id,
                    otherUid = other,
                    otherUsername = uname,
                    otherPicMsgId = pic,
                    lastMessage = (d.get("lastMessage") as? String) ?: "",
                    lastAtMs = lastAt,
                    unreadForMe = unread
                )
            }
            list.sortedByDescending { it.lastAtMs }
        }.getOrDefault(emptyList())
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toDictionary():
        Map<String, Any?> = this.data ?: emptyMap()
}
