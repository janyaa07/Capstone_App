package com.aireventure.auth.AuthFiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aireventure.auth.AuthFiles.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val idToken: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("All fields are required.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.signUp(email, password)
                .onSuccess { _authState.value = AuthState.Success() }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Signup failed.") }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password are required.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.login(email, password)
                .onSuccess { session ->
                    _authState.value = AuthState.Success(
                        idToken = session.idToken.jwtToken
                    )
                }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Login failed.") }
        }
    }
}