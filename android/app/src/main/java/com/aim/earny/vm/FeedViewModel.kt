package com.aim.earny.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.earny.BuildConfig
import com.aim.earny.data.ApiService
import com.aim.earny.data.Video
import com.aim.earny.data.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FeedViewModel : ViewModel() {
    private val api: ApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE.trimEnd('/') + "/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ApiService::class.java)

    private val repo = VideoRepository(api)

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos = _videos.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _videos.value = repo.feed()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _loading.value = false
            }
        }
    }

    fun markViewed(id: String) = viewModelScope.launch { repo.markViewed(id) }
    fun like(id: String) = viewModelScope.launch { runCatching { repo.like(id) } }
}
