package com.taskify.pro.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around the raw Firebase SDKs.
 *
 * Every method is a `suspend fun` that bridges the callback-based Firebase
 * APIs to coroutines via `.await()` from the coroutines-play-services artifact.
 *
 * Keeping this layer separate from the repositories makes it trivial to swap
 * Firebase for another backend in tests or future migrations.
 */
@Singleton
class FirebaseSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    // ── Collection & field names ──────────────────────────────────────────
    companion object {
        const val COLLECTION_TASKS = "tasks"
        const val FIELD_USER_ID = "user_id"
        const val FIELD_COMPLETED = "completed"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_CREATED_AT = "created_at"
    }

    // =====================================================================
    //  AUTHENTICATION
    // =====================================================================

    /** Returns the currently signed-in user (may be null). */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** Check whether a user session exists. */
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Sign up with email & password.
     * @throws Exception propagated from Firebase (e.g. weak password, email in use).
     */
    suspend fun signUp(email: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user ?: throw Exception("Sign-up succeeded but user is null")
    }

    /**
     * Sign in with email & password.
     * @throws Exception propagated from Firebase (e.g. wrong password, user not found).
     */
    suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: throw Exception("Sign-in succeeded but user is null")
    }

    /**
     * Send a password-reset email to [email].
     */
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Delete the currently signed-in user account.
     */
    suspend fun deleteAccount() {
        auth.currentUser?.delete()?.await()
            ?: throw Exception("No user is currently signed in")
    }

    // =====================================================================
    //  FIRESTORE — TASKS
    // =====================================================================

    /**
     * Get a query that fetches only the current user's tasks, ordered by creation date.
     */
    fun getTasksQuery(): Query {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to fetch tasks")
        return firestore.collection(COLLECTION_TASKS)
            .whereEqualTo(FIELD_USER_ID, uid)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
    }

    /**
     * Add a new task document to Firestore.
     * @return The auto-generated document ID.
     */
    suspend fun addTask(task: Map<String, Any?>): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to add tasks")
        val taskWithUser = task.toMutableMap().apply { put(FIELD_USER_ID, uid) }
        val docRef = firestore.collection(COLLECTION_TASKS).add(taskWithUser).await()
        return docRef.id
    }

    /**
     * Update specific fields of an existing task document.
     */
    suspend fun updateTask(taskId: String, updates: Map<String, Any?>) {
        firestore.collection(COLLECTION_TASKS).document(taskId).update(updates).await()
    }

    /**
     * Toggle the `completed` status of a task.
     */
    suspend fun toggleTaskCompletion(taskId: String, completed: Boolean) {
        firestore.collection(COLLECTION_TASKS)
            .document(taskId)
            .update(FIELD_COMPLETED, completed)
            .await()
    }

    /**
     * Permanently delete a task document.
     */
    suspend fun deleteTask(taskId: String) {
        firestore.collection(COLLECTION_TASKS).document(taskId).delete().await()
    }
}
