package com.aim.earny.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.earny.BuildConfig
import com.aim.earny.data.ApiService
import com.aim.earny.data.DocumentMapper
import com.aim.earny.data.FollowRepository
import com.aim.earny.data.Video
import com.aim.earny.data.VideoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FeedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val api: ApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE.trimEnd('/') + "/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    private val videoRepo = VideoRepository(api)
    private val followRepo = FollowRepository()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos = _videos.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** IDs of videos I've liked */
    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds = _likedIds.asStateFlow()

    /** UIDs I'm currently following */
    private val _followingIds = MutableStateFlow<Set<String>>(emptySet())
    val followingIds = _followingIds.asStateFlow()

    /** Video IDs we've already counted a view for this session */
    private val viewedIds = mutableSetOf<String>()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // Load videos
                val snap = db.collection("videos")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
                _videos.value = snap.documents.map { DocumentMapper.video(it) }

                // Load my follows (for the +/✓ toggle + "Following" tab)
                val me = auth.currentUser?.uid
                if (me != null) {
                    val followsSnap = db.collection("follows")
                        .whereEqualTo("follower", me)
                        .get()
                        .await()
                    _followingIds.value = followsSnap.documents
                        .mapNotNull { it.getString("followee") }
                        .toSet()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load feed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun onPageVisible(video: Video) {
        // Like state
        viewModelScope.launch {
            val liked = runCatching { videoRepo.hasLiked(video.id) }.getOrDefault(false)
            if (liked) _likedIds.value = _likedIds.value + video.id
        }
        // View count
        if (viewedIds.add(video.id)) {
            viewModelScope.launch {
                videoRepo.incrementView(video.id)
                _videos.value = _videos.value.map {
                    if (it.id == video.id) it.copy(views = it.views + 1) else it
                }
            }
        }
    }

    fun toggleLike(video: Video) {
        val wasLiked = _likedIds.value.contains(video.id)
        val optimisticLiked = !wasLiked

        _likedIds.value = if (optimisticLiked)
            _likedIds.value + video.id else _likedIds.value - video.id
        _videos.value = _videos.value.map {
            if (it.id == video.id) {
                it.copy(likes = (it.likes + if (optimisticLiked) 1 else -1).coerceAtLeast(0))
            } else it
        }

        viewModelScope.launch {
            try {
                val actual = videoRepo.toggleLike(video.id)
                _likedIds.value = if (actual)
                    _likedIds.value + video.id else _likedIds.value - video.id
            } catch (_: Exception) {
                _likedIds.value = if (wasLiked)
                    _likedIds.value + video.id else _likedIds.value - video.id
                _videos.value = _videos.value.map {
                    if (it.id == video.id) {
                        it.copy(likes = (it.likes + if (wasLiked) 1 else -1).coerceAtLeast(0))
                    } else it
                }
            }
        }
    }

    /** Toggle follow of a video's uploader (persists to Firestore) */
    fun toggleFollow(uploaderUid: String) {
        val me = auth.currentUser?.uid ?: return
        if (uploaderUid == me) return
        if (uploaderUid.isBlank()) return

        val wasFollowing = _followingIds.value.contains(uploaderUid)
        val optimistic = !wasFollowing

        _followingIds.value = if (optimistic)
            _followingIds.value + uploaderUid else _followingIds.value - uploaderUid

        viewModelScope.launch {
            try {
                val actual = followRepo.toggleFollow(uploaderUid)
                _followingIds.value = if (actual)
                    _followingIds.value + uploaderUid else _followingIds.value - uploaderUid
            } catch (_: Exception) {
                // Revert
                _followingIds.value = if (wasFollowing)
                    _followingIds.value + uploaderUid else _followingIds.value - uploaderUid
            }
        }
    }

    /** Videos from users I follow only */
    fun followingFeed(): List<Video> {
        val following = _followingIds.value
        return _videos.value.filter { following.contains(it.uploader) }
    }
}
