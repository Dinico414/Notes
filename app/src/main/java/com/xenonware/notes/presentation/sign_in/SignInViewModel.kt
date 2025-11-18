package com.xenonware.notes.presentation.sign_in

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xenonware.notes.SharedPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SignInViewModel(application: Application): ViewModel() {

    private val sharedPreferenceManager = SharedPreferenceManager(application)

    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                isSignInSuccessful = sharedPreferenceManager.isUserLoggedIn
            )
        }
    }

    fun onSignInResult(result: SignInResult) {
        _state.update {
            it.copy(
                isSignInSuccessful = result.data != null,
                signInError = result.errorMessage
            )
        }
        sharedPreferenceManager.isUserLoggedIn = result.data != null
    }

    fun resetState() {
        _state.update { SignInState() }
        sharedPreferenceManager.isUserLoggedIn = false
    }

    class SignInViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SignInViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SignInViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}