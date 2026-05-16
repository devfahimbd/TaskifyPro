package com.taskify.pro.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.Date

/**
 * Task data model mapped directly to a Firestore document.
 *
 * Firestore fields use snake_case; @PropertyName annotations map them
 * to the Kotlin camelCase properties so the rest of the codebase stays clean.
 */
@IgnoreExtraProperties
data class Task(
    @get:Exclude var documentId: String = "",

    @PropertyName("user_id")
    var userId: String = "",

    var title: String = "",

    var description: String = "",

    @ServerTimestamp
    var timestamp: Date? = null,

    var completed: Boolean = false,

    @PropertyName("created_at")
    @ServerTimestamp
    var createdAt: Date? = null
) : Serializable {

    /**
     * Helper — true when the task reminder time has already passed.
     */
    @get:Exclude
    val isOverdue: Boolean
        get() = timestamp?.before(Date()) == true && !completed

    /**
     * Human-readable time remaining, or "Overdue" / "Completed".
     */
    @get:Exclude
    val timeRemainingText: String
        get() {
            if (completed) return "Completed"
            val target = timestamp ?: return "No due time"
            val now = Date()
            val diff = target.time - now.time
            return when {
                diff <= 0 -> "Overdue"
                diff < 60_000 -> "Due now"
                diff < 3_600_000 -> "${diff / 60_000} min left"
                diff < 86_400_000 -> "${diff / 3_600_000} hr left"
                else -> "${diff / 86_400_000} days left"
            }
        }
}
