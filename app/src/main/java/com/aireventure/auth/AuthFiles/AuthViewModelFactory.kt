package com.aireventure.auth.AuthFiles

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aireventure.auth.AuthFiles.AuthRepository

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = AuthRepository(context)
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(repository) as T
    }
}