package com.aim.earny.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.earny.BuildConfig
import com.aim.earny.data.ApiService
import com.aim.earny.data.DocumentMapper
import com.aim.earny.data.Video
import com.aim.earny.data.VideoRepository
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

    private val api: ApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE.trimEnd('/') + "/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    private val videoRepo = VideoRepository(api)

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos = _videos.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** Video IDs the current user has liked (in-memory cache) */
    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds = _likedIds.asStateFlow()

    /** Video IDs we've already counted a view for this session */
    private val viewedIds = mutableSetOf<String>()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val snap = db.collection("videos")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
                _videos.value = snap.documents.map { DocumentMapper.video(it) }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load feed"
            } finally {
                _loading.value = false
            }
        }
    }

    /** Called when a page becomes the active one. Marks a view + loads like state. */
    fun onPageVisible(video: Video) {
        // Likes: check once per session
        viewModelScope.launch {
            val liked = runCatching { videoRepo.hasLiked(video.id) }.getOrDefault(false)
            if (liked) {
                _likedIds.value = _likedIds.value + video.id
            }
        }
        // Views: increment once per session per video
        if (viewedIds.add(video.id)) {
            viewModelScope.launch {
                videoRepo.incrementView(video.id)
                // Optimistic UI bump
                _videos.value = _videos.value.map {
                    if (it.id == video.id) it.copy(views = it.views + 1) else it
                }
            }
        }
    }

    /** Toggle like. Optimistic + reconcile. */
    fun toggleLike(video: Video) {
        val wasLiked = _likedIds.value.contains(video.id)
        val optimisticLiked = !wasLiked

        // Optimistic UI update
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
                // Reconcile in case of drift
                _likedIds.value = if (actual)
                    _likedIds.value + video.id else _likedIds.value - video.id
            } catch (_: Exception) {
                // Revert on failure
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
}
