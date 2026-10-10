package com.aim.earny.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.earny.data.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UsernameState { Idle, Checking, Available, Taken, Invalid }

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _info = MutableStateFlow<String?>(null)
    val info = _info.asStateFlow()

    private val _usernameState = MutableStateFlow(UsernameState.Idle)
    val usernameState = _usernameState.asStateFlow()

    private var checkJob: Job? = null

    fun clearError() { _error.value = null; _info.value = null }

    /** Debounced username availability check */
    fun checkUsername(username: String) {
        checkJob?.cancel()
        val clean = username.trim().lowercase()

        when {
            clean.isEmpty() -> { _usernameState.value = UsernameState.Idle; return }
            clean.length < 3 -> { _usernameState.value = UsernameState.Invalid; return }
            !clean.matches(Regex("^[a-z0-9_.]+$")) -> {
                _usernameState.value = UsernameState.Invalid; return
            }
        }

        _usernameState.value = UsernameState.Checking
        checkJob = viewModelScope.launch {
            delay(450) // debounce
            try {
                val ok = repo.isUsernameAvailable(clean)
                _usernameState.value = if (ok) UsernameState.Available else UsernameState.Taken
            } catch (e: Exception) {
                // Network / permission error — do NOT mark as "taken"
                _usernameState.value = UsernameState.Idle
                _error.value = "Couldn't check username: ${e.message}"
            }
        }
    }

    fun signUp(
        first: String, last: String, username: String,
        email: String, password: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                repo.signUpWithDetails(first, last, username, email, password)
                onSuccess()
            } catch (e: Exception) {
                _error.value = repo.friendlyError(e)
            } finally { _loading.value = false }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                repo.signIn(email, password)
                onSuccess()
            } catch (e: Exception) {
                _error.value = repo.friendlyError(e)
            } finally { _loading.value = false }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                repo.resetPassword(email)
                _info.value = "Password reset email sent"
            } catch (e: Exception) {
                _error.value = repo.friendlyError(e)
            } finally { _loading.value = false }
        }
    }

    suspend fun checkVerified(): Boolean =
        runCatching { repo.checkVerified() }.getOrDefault(false)

    fun resend() {
        viewModelScope.launch { runCatching { repo.resendVerification() } }
    }

    fun signOut() = repo.signOut()
}
