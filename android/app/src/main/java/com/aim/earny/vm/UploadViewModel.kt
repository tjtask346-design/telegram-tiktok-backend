package com.aim.earny.vm

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.earny.BuildConfig
import com.aim.earny.data.ApiService
import com.aim.earny.data.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    object Done : UploadState()
    data class Error(val msg: String) : UploadState()
}

class UploadViewModel : ViewModel() {
    private val api: ApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE.trimEnd('/') + "/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ApiService::class.java)

    private val repo = VideoRepository(api)

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state = _state.asStateFlow()

    fun upload(ctx: Context, uri: Uri, caption: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = UploadState.Uploading
            try {
                val mmr = MediaMetadataRetriever().apply { setDataSource(ctx, uri) }
                val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.div(1000)?.toInt() ?: 0
                val w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0
                val h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
                mmr.release()

                repo.upload(ctx, uri, caption, dur, w, h)
                _state.value = UploadState.Done
                onDone()
            } catch (e: Exception) {
                _state.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun reset() { _state.value = UploadState.Idle }
}
