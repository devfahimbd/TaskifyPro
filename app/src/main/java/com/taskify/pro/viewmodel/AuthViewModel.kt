package com.taskify.pro.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.taskify.pro.repository.AuthRepository
import com.taskify.pro.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the authentication screens (Login / Sign-Up).
 *
 * Exposes [LiveData] streams for sign-up, sign-in and password-reset states
 * so the UI can observe and react to loading, success and error events.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // ── Public observable state ───────────────────────────────────────────

    /** Live state for the sign-up flow. */
    private val _signUpState = MutableLiveData<Resource<FirebaseUser>>()
    val signUpState: LiveData<Resource<FirebaseUser>> = _signUpState

    /** Live state for the sign-in flow. */
    private val _signInState = MutableLiveData<Resource<FirebaseUser>>()
    val signInState: LiveData<Resource<FirebaseUser>> = _signInState

    /** Live state for the password-reset flow. */
    private val _resetState = MutableLiveData<Resource<Unit>>()
    val resetState: LiveData<Resource<Unit>> = _resetState

    // =====================================================================
    //  Public API
    // =====================================================================

    /**
     * Register a new account with the given credentials.
     */
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _signUpState.value = Resource.Loading()
            _signUpState.value = authRepository.signUp(email, password)
        }
    }

    /**
     * Log in to an existing account.
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _signInState.value = Resource.Loading()
            _signInState.value = authRepository.signIn(email, password)
        }
    }

    /**
     * Trigger a password-reset email for [email].
     */
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _resetState.value = Resource.Loading()
            _resetState.value = authRepository.sendPasswordReset(email)
        }
    }

    /**
     * Sign out the current user and clear all auth-related state.
     */
    fun signOut() {
        authRepository.signOut()
    }

    /**
     * Check whether a user is currently authenticated.
     */
    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()
}
