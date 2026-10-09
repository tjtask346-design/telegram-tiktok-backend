package com.aim.earny.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.earny.data.DocumentMapper
import com.aim.earny.data.UserProfile
import com.aim.earny.data.Video
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class ProfileTab { VIDEOS, PRIVATE, REPOSTS, LIKED }

class ProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile = _profile.asStateFlow()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos = _videos.asStateFlow()

    private val _privateVideos = MutableStateFlow<List<Video>>(emptyList())
    val privateVideos = _privateVideos.asStateFlow()

    private val _reposts = MutableStateFlow<List<Video>>(emptyList())
    val reposts = _reposts.asStateFlow()

    private val _liked = MutableStateFlow<List<Video>>(emptyList())
    val liked = _liked.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _isOwnProfile = MutableStateFlow(true)
    val isOwnProfile = _isOwnProfile.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing = _isFollowing.asStateFlow()

    private var currentTargetUid: String? = null

    fun load(targetUid: String? = null) {
        val me = auth.currentUser?.uid
        val uid = targetUid ?: me ?: run {
            _loading.value = false
            return
        }
        currentTargetUid = uid
        _isOwnProfile.value = uid == me

        viewModelScope.launch {
            _loading.value = true

            // Profile — safe mapping, never throws
            runCatching {
                val doc = db.collection("users").document(uid).get().await()
                _profile.value = DocumentMapper.user(doc, uid)
            }

            // Videos — safe mapping
            runCatching {
                val snap = db.collection("videos")
                    .whereEqualTo("uploader", uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(60)
                    .get()
                    .await()

                val all = snap.documents.map { DocumentMapper.video(it) }
                _videos.value = all.filter { !it.isPrivate && !it.isDraft && !it.isRepost }
                _privateVideos.value = all.filter { it.isPrivate || it.isDraft }
                _reposts.value = all.filter { it.isRepost }
            }

            _liked.value = emptyList()
            _loading.value = false
        }
    }

    fun togglePin(video: Video) {
        viewModelScope.launch {
            runCatching {
                db.collection("videos").document(video.id)
                    .update("isPinned", !video.isPinned).await()
                load(currentTargetUid)
            }
        }
    }

    fun togglePrivacy(video: Video) {
        viewModelScope.launch {
            runCatching {
                db.collection("videos").document(video.id)
                    .update("isPrivate", !video.isPrivate).await()
                load(currentTargetUid)
            }
        }
    }

    fun deleteVideo(video: Video) {
        viewModelScope.launch {
            runCatching {
                db.collection("videos").document(video.id).delete().await()
                load(currentTargetUid)
            }
        }
    }

    fun toggleFollow() {
        val target = currentTargetUid ?: return
        val me = auth.currentUser?.uid ?: return
        if (target == me) return

        viewModelScope.launch {
            val nowFollowing = !_isFollowing.value
            _isFollowing.value = nowFollowing
            runCatching {
                db.collection("users").document(target).update(
                    "followers",
                    FieldValue.increment(if (nowFollowing) 1 else -1)
                ).await()
                db.collection("users").document(me).update(
                    "following",
                    FieldValue.increment(if (nowFollowing) 1 else -1)
                ).await()
            }.onFailure { _isFollowing.value = !nowFollowing }
        }
    }
}
