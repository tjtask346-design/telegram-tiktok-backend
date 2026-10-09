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

    private val _verified = MutableStateFlow<Boolean?>(null)
    val verified = _verified.asStateFlow()

    fun clearError() { _error.value = null }

    fun sendMagicLink(email: String, landingUrl: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                repo.sendMagicLink(email, landingUrl)
                onDone()
            } catch (e: Exception) {
                _error.value = repo.friendlyError(e)
            } finally { _loading.value = false }
        }
    }

    fun signInWithLink(email: String, link: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                repo.signInWithEmailLink(email, link)
                onDone()
            } catch (e: Exception) {
                _error.value = repo.friendlyError(e)
            } finally { _loading.value = false }
        }
    }

    fun signUpWithPassword(email: String, password: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                repo.signUpWithPassword(email, password)
                onDone()
            } catch (e: Exception) {
                _error.value = repo.friendlyError(e)
            } finally { _loading.value = false }
        }
    }

    fun signInWithPassword(email: String, password: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                repo.signInWithPassword(email, password)
                onDone()
            } catch (e: Exception) {
                _error.value = repo.friendlyError(e)
            } finally { _loading.value = false }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try { repo.resetPassword(email) }
            catch (e: Exception) { _error.value = repo.friendlyError(e) }
            finally { _loading.value = false }
        }
    }

    fun updateProfile(username: String, bio: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                repo.updateProfile(username, bio)
                onDone()
            } catch (e: Exception) {
                _error.value = repo.friendlyError(e)
            } finally { _loading.value = false }
        }
    }

    fun pollVerification() {
        viewModelScope.launch {
            _verified.value = runCatching { repo.checkVerified() }.getOrDefault(false)
        }
    }

    fun signOut() = repo.signOut()
}
