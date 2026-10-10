package com.aim.earny.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.earny.BuildConfig
import com.aim.earny.data.ApiService
import com.aim.earny.data.BlockRepository
import com.aim.earny.data.BookmarkRepository
import com.aim.earny.data.DocumentMapper
import com.aim.earny.data.FollowRepository
import com.aim.earny.data.Video
import com.aim.earny.data.VideoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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
    private val bookmarkRepo = BookmarkRepository()
    private val blockRepo = BlockRepository()

    private val PAGE_SIZE = 10L

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos = _videos.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore = _loadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore = _hasMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds = _likedIds.asStateFlow()

    private val _followingIds = MutableStateFlow<Set<String>>(emptySet())
    val followingIds = _followingIds.asStateFlow()

    private val _blockedUids = MutableStateFlow<Set<String>>(emptySet())
    val blockedUids = _blockedUids.asStateFlow()

    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedIds = _bookmarkedIds.asStateFlow()

    private var lastDoc: DocumentSnapshot? = null
    private val viewedIds = mutableSetOf<String>()

    init { load() }

    /** Fresh load from top — resets pagination */
    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _hasMore.value = true
            lastDoc = null

            try {
                // Videos — page 1
                val snap = db.collection("videos")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE)
                    .get()
                    .await()
                _videos.value = snap.documents.map { DocumentMapper.video(it) }
                lastDoc = snap.documents.lastOrNull()
                _hasMore.value = snap.size() >= PAGE_SIZE

                // Load blocked uids
                val meB0 = auth.currentUser?.uid
                if (meB0 != null) {
                    runCatching { _blockedUids.value = blockRepo.myBlockedUids() }
                }

                // Load my bookmarks + follows
                val me = auth.currentUser?.uid
                if (me != null) {
                    runCatching {
                        val bSnap = db.collection("users").document(me)
                            .collection("bookmarks").get().await()
                        _bookmarkedIds.value = bSnap.documents.map { it.id }.toSet()
                    }
                    runCatching {
                        val fSnap = db.collection("follows")
                            .whereEqualTo("follower", me).get().await()
                        _followingIds.value = fSnap.documents
                            .mapNotNull { it.getString("followee") }.toSet()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load feed"
            } finally {
                _loading.value = false
            }
        }
    }

    /** Load the next page — called when user nears the end of the feed */
    fun loadMore() {
        if (_loadingMore.value || !_hasMore.value) return
        val anchor = lastDoc ?: return

        viewModelScope.launch {
            _loadingMore.value = true
            try {
                val snap = db.collection("videos")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .startAfter(anchor)
                    .limit(PAGE_SIZE)
                    .get()
                    .await()

                val newVideos = snap.documents.map { DocumentMapper.video(it) }
                // Dedupe just in case
                val existingIds = _videos.value.map { it.id }.toSet()
                val fresh = newVideos.filter { it.id !in existingIds }

                _videos.value = _videos.value + fresh
                lastDoc = snap.documents.lastOrNull() ?: anchor
                _hasMore.value = snap.size() >= PAGE_SIZE
            } catch (_: Exception) {
                // Silent — UI stays on current page
            } finally {
                _loadingMore.value = false
            }
        }
    }

    fun onPageVisible(video: Video) {
        viewModelScope.launch {
            val liked = runCatching { videoRepo.hasLiked(video.id) }.getOrDefault(false)
            if (liked) _likedIds.value = _likedIds.value + video.id
        }
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
        val optimistic = !wasLiked

        _likedIds.value = if (optimistic)
            _likedIds.value + video.id else _likedIds.value - video.id
        _videos.value = _videos.value.map {
            if (it.id == video.id) {
                it.copy(likes = (it.likes + if (optimistic) 1 else -1).coerceAtLeast(0))
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

    fun toggleFollow(uploaderUid: String) {
        val me = auth.currentUser?.uid ?: return
        if (uploaderUid == me || uploaderUid.isBlank()) return

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
                _followingIds.value = if (wasFollowing)
                    _followingIds.value + uploaderUid else _followingIds.value - uploaderUid
            }
        }
    }

    fun toggleBookmark(video: Video) {
        val wasBookmarked = _bookmarkedIds.value.contains(video.id)
        val optimistic = !wasBookmarked

        _bookmarkedIds.value = if (optimistic)
            _bookmarkedIds.value + video.id else _bookmarkedIds.value - video.id

        viewModelScope.launch {
            try {
                val actual = bookmarkRepo.toggle(video.id)
                _bookmarkedIds.value = if (actual)
                    _bookmarkedIds.value + video.id else _bookmarkedIds.value - video.id
            } catch (_: Exception) {
                _bookmarkedIds.value = if (wasBookmarked)
                    _bookmarkedIds.value + video.id else _bookmarkedIds.value - video.id
            }
        }
    }

    fun followingFeed(): List<Video> {
        val following = _followingIds.value
        return _videos.value.filter { following.contains(it.uploader) }
    }


    fun toggleBlock(targetUid: String) {
        if (targetUid.isBlank()) return
        viewModelScope.launch {
            try {
                val nowBlocked = blockRepo.toggle(targetUid)
                _blockedUids.value = if (nowBlocked)
                    _blockedUids.value + targetUid else _blockedUids.value - targetUid
                // Refresh feed to hide/show their videos
                load()
            } catch (_: Exception) {}
        }
    }

    fun isBlocked(uid: String): Boolean = _blockedUids.value.contains(uid)
}
