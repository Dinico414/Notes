package com.xenonware.notes.presentation.sign_in

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xenonware.notes.SharedPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SignInViewModel(
    application: Application,
    private val sharedPreferenceManager: SharedPreferenceManager = SharedPreferenceManager(application)
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "is_user_logged_in") {
            val isLoggedIn = prefs.getBoolean(key, false)
            _state.update { it.copy(isSignInSuccessful = isLoggedIn) }
        }
    }

    init {
        sharedPreferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        // Optional: Initial sync if needed
        val isLoggedIn = sharedPreferenceManager.isUserLoggedIn
        _state.update { it.copy(isSignInSuccessful = isLoggedIn) }
    }

    override fun onCleared() {
        sharedPreferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        super.onCleared()
    }

    fun updateSignInState(isSignedIn: Boolean) {
        _state.update { it.copy(isSignInSuccessful = isSignedIn) }
    }

    fun onSignInResult(result: SignInResult) {
        _state.update {
            if (result.data != null) {
                sharedPreferenceManager.isUserLoggedIn = true
                it.copy(
                    isSignInSuccessful = true,
                    signInError = null
                )
            } else {
                it.copy(
                    isSignInSuccessful = false,
                    signInError = result.errorMessage
                )
            }
        }
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