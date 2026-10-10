package com.aim.earny.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Simple event bus for cross-screen signals (upload done, profile saved).
 * Uses monotonically increasing ints — screens observe and reload.
 */
object AppEvents {

    // ---- Feed ----
    private val _feedRefresh = MutableStateFlow(0)
    val feedRefresh: StateFlow<Int> = _feedRefresh

    fun triggerFeedRefresh() {
        _feedRefresh.value = _feedRefresh.value + 1
    }

    // ---- Profile ----
    private val _profileRefresh = MutableStateFlow(0)
    val profileRefresh: StateFlow<Int> = _profileRefresh

    fun triggerProfileRefresh() {
        _profileRefresh.value = _profileRefresh.value + 1
    }
}
