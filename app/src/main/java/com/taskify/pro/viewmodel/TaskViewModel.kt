package com.taskify.pro.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskify.pro.model.Task
import com.taskify.pro.repository.TaskRepository
import com.taskify.pro.utils.NotificationHelper
import com.taskify.pro.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel that drives the main task list screen.
 *
 * Responsibilities:
 *  - Observe the real-time task list from Firestore.
 *  - Provide CRUD operations (add / update / toggle / delete).
 *  - Manage notification scheduling whenever a task is created, updated, or deleted.
 *  - Split the flat list into "pending" and "completed" sections for the UI.
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    // ── Observable task list (single source of truth) ─────────────────────

    private val _tasksState = MutableStateFlow<Resource<List<Task>>>(Resource.Loading())
    val tasksState: StateFlow<Resource<List<Task>>> = _tasksState.asStateFlow()

    // ── One-shot operation states ─────────────────────────────────────────

    private val _operationState = MutableLiveData<Resource<*>>()
    val operationState: LiveData<Resource<*>> = _operationState

    // =====================================================================
    //  Initialisation — start listening on first collection
    // =====================================================================

    init {
        observeTasks()
    }

    private fun observeTasks() {
        viewModelScope.launch {
            taskRepository.observeTasks().collect { resource ->
                _tasksState.value = resource
            }
        }
    }

    // =====================================================================
    //  Derived lists for the UI sections
    // =====================================================================

    /** Tasks that are NOT yet completed, sorted by timestamp. */
    val pendingTasks: List<Task>
        get() = (_tasksState.value as? Resource.Success)?.data
            ?.filter { !it.completed }
            ?.sortedBy { it.timestamp }
            ?: emptyList()

    /** Tasks that ARE completed, sorted by creation date (most recent first). */
    val completedTasks: List<Task>
        get() = (_tasksState.value as? Resource.Success)?.data
            ?.filter { it.completed }
            ?: emptyList()

    val hasPendingTasks: Boolean get() = pendingTasks.isNotEmpty()
    val hasCompletedTasks: Boolean get() = completedTasks.isNotEmpty()

    // =====================================================================
    //  CRUD operations
    // =====================================================================

    /**
     * Add a new task and schedule its notification.
     *
     * @param title       Mandatory task title.
     * @param description Optional description.
     * @param reminderAt  When the notification should fire.
     */
    fun addTask(
        context: android.content.Context,
        title: String,
        description: String,
        reminderAt: Date
    ) {
        viewModelScope.launch {
            _operationState.value = Resource.Loading()

            val firebaseTimestamp = com.google.firebase.Timestamp(reminderAt)

            when (val result = taskRepository.addTask(title, description, firebaseTimestamp)) {
                is Resource.Success -> {
                    // Schedule the local notification alarm.
                    NotificationHelper.scheduleTaskReminder(
                        context,
                        result.data,
                        title,
                        reminderAt
                    )
                    _operationState.value = Resource.Success(result.data)
                }
                is Resource.Error -> {
                    _operationState.value = Resource.Error(result.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Update an existing task and reschedule its notification.
     */
    fun updateTask(
        context: android.content.Context,
        taskId: String,
        title: String,
        description: String,
        reminderAt: Date
    ) {
        viewModelScope.launch {
            _operationState.value = Resource.Loading()

            val firebaseTimestamp = com.google.firebase.Timestamp(reminderAt)

            when (taskRepository.updateTask(taskId, title, description, firebaseTimestamp)) {
                is Resource.Success -> {
                    // Cancel old alarm and schedule new one.
                    NotificationHelper.cancelTaskReminder(context, taskId)
                    NotificationHelper.scheduleTaskReminder(context, taskId, title, reminderAt)
                    _operationState.value = Resource.Success(Unit)
                }
                is Resource.Error -> {
                    _operationState.value = Resource.Error(it.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Toggle the completed/pending status of a task.
     * If the task is being marked completed, cancel its reminder notification.
     */
    fun toggleTaskCompletion(
        context: android.content.Context,
        taskId: String,
        completed: Boolean
    ) {
        viewModelScope.launch {
            when (taskRepository.toggleTaskCompletion(taskId, completed)) {
                is Resource.Success -> {
                    if (completed) {
                        // No more reminders needed for completed tasks.
                        NotificationHelper.cancelTaskReminder(context, taskId)
                    }
                }
                is Resource.Error -> {
                    // Optionally surface the error to the UI.
                }
                else -> {}
            }
        }
    }

    /**
     * Delete a task and cancel its notification alarm.
     */
    fun deleteTask(
        context: android.content.Context,
        taskId: String
    ) {
        viewModelScope.launch {
            when (taskRepository.deleteTask(taskId)) {
                is Resource.Success -> {
                    NotificationHelper.cancelTaskReminder(context, taskId)
                }
                is Resource.Error -> {
                    _operationState.value = Resource.Error(it.message)
                }
                else -> {}
            }
        }
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    /**
     * Clear the one-shot operation state so the UI can stop showing
     * a loading spinner or error message.
     */
    fun clearOperationState() {
        _operationState.value = null
    }
}
