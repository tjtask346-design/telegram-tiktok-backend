package com.aim.earny.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.earny.BuildConfig
import com.aim.earny.data.ApiService
import com.aim.earny.data.DocumentMapper
import com.aim.earny.data.VideoRepository
import com.aim.earny.data.FollowRepository
import com.aim.earny.data.UserProfile
import com.aim.earny.data.Video
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

enum class ProfileTab { VIDEOS, PRIVATE, REPOSTS, LIKED, SAVED }

class ProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val followRepo = FollowRepository(auth, db)
    private val videoRepo: VideoRepository by lazy {
        val api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE.trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        VideoRepository(api, auth, db)
    }

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile = _profile.asStateFlow()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos = _videos.asStateFlow()

    private val _privateVideos = MutableStateFlow<List<Video>>(emptyList())
    val privateVideos = _privateVideos.asStateFlow()

    private val _reposts = MutableStateFlow<List<Video>>(emptyList())
    val reposts = _reposts.asStateFlow()

    private val _liked = MutableStateFlow<List<Video>>(emptyList())
    private val _saved = MutableStateFlow<List<Video>>(emptyList())
    val liked = _liked.asStateFlow()
    val saved = _saved.asStateFlow()

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

            // Profile doc
            runCatching {
                val doc = db.collection("users").document(uid).get().await()
                _profile.value = DocumentMapper.user(doc, uid)
            }

            // Videos — simple query, NO orderBy (avoids composite index)
            runCatching {
                val snap = db.collection("videos")
                    .whereEqualTo("uploader", uid)
                    .get()
                    .await()

                val all = snap.documents
                    .map { DocumentMapper.video(it) }
                    // sort newest first: prefer createdAtMs, fallback to id (Firestore ids are time-based)
                    .sortedWith(
                        compareByDescending<Video> { it.createdAtMs }
                            .thenByDescending { it.id }
                    )

                _videos.value = all.filter { !it.isPrivate && !it.isDraft && !it.isRepost }
                _privateVideos.value = all.filter { it.isPrivate || it.isDraft }
                _reposts.value = all.filter { it.isRepost }
            }

            _liked.value = emptyList()

            // Saved: only for own profile
            if (_isOwnProfile.value) {
                runCatching {
                    _saved.value = videoRepo.getSavedVideos()
                }
            } else {
                _saved.value = emptyList()
            }

            // Check follow state if viewing someone else
            if (!_isOwnProfile.value && currentTargetUid != null) {
                _isFollowing.value = runCatching {
                    followRepo.isFollowing(currentTargetUid!!)
                }.getOrDefault(false)
            } else {
                _isFollowing.value = false
            }

            _loading.value = false
        }
    }

    fun toggleFollow() {
        val target = currentTargetUid ?: return
        val me = auth.currentUser?.uid ?: return
        if (target == me) return

        viewModelScope.launch {
            val optimistic = !_isFollowing.value
            _isFollowing.value = optimistic
            try {
                val actual = followRepo.toggleFollow(target)
                _isFollowing.value = actual
                // Refresh counters on target and self
                load(target)
            } catch (e: Exception) {
                // Revert on failure
                _isFollowing.value = !optimistic
            }
        }
    }
}
