package com.aim.earny.vm

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.earny.BuildConfig
import com.aim.earny.data.ApiService
import com.aim.earny.data.UploadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

sealed class UploadState {
    object Idle : UploadState()
    data class Uploading(val progress: Float) : UploadState()
    data class Done(val msgId: Long) : UploadState()
    data class Error(val msg: String) : UploadState()
}

class UploadViewModel : ViewModel() {

    private val api: ApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE.trimEnd('/') + "/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    private val repo = UploadRepository(api)

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state = _state.asStateFlow()

    fun upload(
        context: Context,
        uri: Uri,
        caption: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = UploadState.Uploading(0f)
            try {
                // Read metadata
                var duration = 0
                var w = 0
                var h = 0
                runCatching {
                    val mmr = MediaMetadataRetriever().apply {
                        setDataSource(context, uri)
                    }
                    duration = mmr
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()?.div(1000)?.toInt() ?: 0
                    w = mmr
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        ?.toIntOrNull() ?: 0
                    h = mmr
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        ?.toIntOrNull() ?: 0
                    mmr.release()
                }

                val result = repo.upload(
                    context, uri, caption, duration, w, h
                ) { p ->
                    _state.value = UploadState.Uploading(p)
                }

                _state.value = UploadState.Done(result.msgId)
                onSuccess()
            } catch (e: Exception) {
                _state.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun reset() { _state.value = UploadState.Idle }
}
