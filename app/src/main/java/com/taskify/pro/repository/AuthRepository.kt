package com.taskify.pro.repository

import com.google.firebase.auth.FirebaseUser
import com.taskify.pro.firebase.FirebaseSource
import com.taskify.pro.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that encapsulates all authentication-related data operations.
 * Activities / ViewModels consume this class — never the raw Firebase SDK.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebase: FirebaseSource
) {

    /**
     * Observe the current Firebase user reactively.
     * Emits the user object on every sign-in / sign-out event.
     */
    val currentUser: FirebaseUser?
        get() = firebase.currentUser

    fun isUserLoggedIn(): Boolean = firebase.isUserLoggedIn()

    /**
     * Sign up a new account.
     * Returns a [Resource] so the ViewModel can expose loading / error states.
     */
    suspend fun signUp(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val user = firebase.signUp(email, password)
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sign-up failed. Please try again.")
        }
    }

    /**
     * Sign in to an existing account.
     */
    suspend fun signIn(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val user = firebase.signIn(email, password)
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed. Please check your credentials.")
        }
    }

    /**
     * Request a password-reset email.
     */
    suspend fun sendPasswordReset(email: String): Resource<Unit> {
        return try {
            firebase.sendPasswordReset(email)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send reset email.")
        }
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        firebase.signOut()
    }

    /**
     * Delete the currently signed-in user's account.
     */
    suspend fun deleteAccount(): Resource<Unit> {
        return try {
            firebase.deleteAccount()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete account.")
        }
    }
}
