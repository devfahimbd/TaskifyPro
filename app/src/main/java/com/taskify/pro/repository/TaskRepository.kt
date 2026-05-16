package com.taskify.pro.repository

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.taskify.pro.firebase.FirebaseSource
import com.taskify.pro.model.Task
import com.taskify.pro.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that encapsulates all task CRUD operations and real-time syncing.
 *
 * Uses Firestore's snapshot listeners via [callbackFlow] so the UI layer
 * gets live updates automatically whenever a task is added, modified, or deleted.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val firebase: FirebaseSource
) {

    // =====================================================================
    //  REAL-TIME LISTENER
    // =====================================================================

    /**
     * Observe the current user's task list in real-time.
     *
     * The Flow emits [Resource] objects so the UI can show loading spinners,
     * error messages, or the actual list of tasks.
     *
     * The Flow completes when the collector is cancelled (e.g. Activity destroyed).
     */
    fun observeTasks(): Flow<Resource<List<Task>>> = callbackFlow {
        // Emit initial loading state
        trySend(Resource.Loading())

        val query = firebase.getTasksQuery()

        val registration: ListenerRegistration = query
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (!isActive) return@addSnapshotListener // Flow already cancelled

                if (error != null) {
                    trySend(Resource.Error(
                        error.message ?: "Failed to fetch tasks"
                    ))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val tasks = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Task::class.java)?.apply {
                                documentId = doc.id
                            }
                        } catch (e: Exception) {
                            null // Skip malformed documents
                        }
                    }
                    trySend(Resource.Success(tasks))
                }
            }

        // Cancel the Firestore listener when the Flow collector is cancelled.
        awaitClose { registration.remove() }
    }

    // =====================================================================
    //  WRITE OPERATIONS
    // =====================================================================

    /**
     * Create a new task in Firestore.
     * Returns the generated document ID on success.
     */
    suspend fun addTask(
        title: String,
        description: String,
        timestamp: com.google.firebase.Timestamp
    ): Resource<String> {
        return try {
            val taskData = hashMapOf(
                "title" to title,
                "description" to description,
                "timestamp" to timestamp,
                "completed" to false
            )
            val docId = firebase.addTask(taskData)
            Resource.Success(docId)
        } catch (e: FirebaseFirestoreException) {
            Resource.Error(e.message ?: "Failed to add task. Please try again.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred.")
        }
    }

    /**
     * Update an existing task's title, description, and/or reminder timestamp.
     */
    suspend fun updateTask(
        taskId: String,
        title: String,
        description: String,
        timestamp: com.google.firebase.Timestamp
    ): Resource<Unit> {
        return try {
            val updates = hashMapOf<String, Any?>(
                "title" to title,
                "description" to description,
                "timestamp" to timestamp
            )
            firebase.updateTask(taskId, updates)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update task.")
        }
    }

    /**
     * Toggle the completed status of a task.
     */
    suspend fun toggleTaskCompletion(taskId: String, completed: Boolean): Resource<Unit> {
        return try {
            firebase.toggleTaskCompletion(taskId, completed)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update task status.")
        }
    }

    /**
     * Permanently delete a task.
     */
    suspend fun deleteTask(taskId: String): Resource<Unit> {
        return try {
            firebase.deleteTask(taskId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete task.")
        }
    }

    /**
     * Fetch a single task by ID (one-shot read).
     */
    suspend fun getTaskById(taskId: String): Resource<Task> {
        return try {
            // We need direct Firestore access here; delegate to FirebaseSource
            // or add a helper. For simplicity we query the collection directly.
            val task = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection(FirebaseSource.COLLECTION_TASKS)
                .document(taskId)
                .get()
                .await()
                .toObject(Task::class.java)
                ?.apply { documentId = taskId }
                ?: throw Exception("Task not found")
            Resource.Success(task)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch task.")
        }
    }
}
