package com.aim.earny.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.earny.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _info = MutableStateFlow<String?>(null)
    val info = _info.asStateFlow()

    fun clearError() { _error.value = null; _info.value = null }

    fun signUp(first: String, last: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                repo.signUpWithDetails(first, last, email, password)
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
