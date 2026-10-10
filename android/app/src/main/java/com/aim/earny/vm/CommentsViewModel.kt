package com.aim.earny.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.earny.data.Comment
import com.aim.earny.data.CommentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentsViewModel(
    private val repo: CommentsRepository = CommentsRepository()
) : ViewModel() {

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments = _comments.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending = _sending.asStateFlow()

    private var currentVideoId: String? = null

    fun load(videoId: String) {
        currentVideoId = videoId
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _comments.value = repo.load(videoId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load comments"
            } finally {
                _loading.value = false
            }
        }
    }

    fun send(text: String, onSent: () -> Unit = {}) {
        val vid = currentVideoId ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            _sending.value = true
            try {
                val c = repo.add(vid, text)
                _comments.value = listOf(c) + _comments.value
                onSent()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _sending.value = false
            }
        }
    }

    fun delete(comment: Comment) {
        val vid = currentVideoId ?: return
        viewModelScope.launch {
            try {
                repo.delete(vid, comment.id)
                _comments.value = _comments.value.filter { it.id != comment.id }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
