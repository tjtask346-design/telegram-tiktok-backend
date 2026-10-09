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

    fun clearError() { _error.value = null }

    fun signUp(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repo.signUpWithDetails(firstName, lastName, email, password)
                onSuccess()
            } catch (e: Exception) {
                _error.value = repo.friendlyError(e)
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun checkVerified(): Boolean =
        runCatching { repo.checkVerified() }.getOrDefault(false)

    fun resend() {
        viewModelScope.launch {
            runCatching { repo.resendVerification() }
        }
    }

    fun signOut() = repo.signOut()
}
