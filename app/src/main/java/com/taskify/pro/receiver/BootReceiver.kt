package com.taskify.pro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.taskify.pro.utils.NotificationHelper
import com.taskify.pro.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

/**
 * BroadcastReceiver that fires when the device boots (or when
 * the user toggles Airplane mode on some devices).
 *
 * It reschedules all pending (non-completed) task alarms so that
 * reminders survive reboots.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted — rescheduling task alarms")

            // Reschedule in a background scope to avoid ANR.
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                rescheduleAllAlarms(context)
            }
        }
    }

    /**
     * Query all pending tasks for the current user and re-register their alarms.
     */
    private suspend fun rescheduleAllAlarms(context: Context) {
        try {
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid ?: return

            val snapshot = FirebaseFirestore.getInstance()
                .collection("tasks")
                .whereEqualTo("user_id", uid)
                .whereEqualTo("completed", false)
                .get()
                .await()

            var rescheduledCount = 0
            for (document in snapshot.documents) {
                val title = document.getString("title") ?: continue
                val timestamp = document.getTimestamp("timestamp") ?: continue
                val dueDate = timestamp.toDate()

                // Only schedule if the reminder is in the future.
                if (dueDate.after(Date())) {
                    NotificationHelper.scheduleTaskReminder(
                        context,
                        document.id,
                        title,
                        dueDate
                    )
                    rescheduledCount++
                }
            }
            Log.d(TAG, "Rescheduled $rescheduledCount alarms after boot")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule alarms after boot", e)
        }
    }

    // Extension to bridge callback to suspend.
    private suspend fun com.google.firebase.firestore.Task<*>.await() =
        kotlinx.coroutines.suspendCancellableCoroutine<com.google.firebase.firestore.QuerySnapshot> { cont ->
            addOnSuccessListener { result ->
                @Suppress("UNCHECKED_CAST")
                cont.resume(result as com.google.firebase.firestore.QuerySnapshot, null)
            }
            addOnFailureListener { error ->
                cont.resumeWithException(error)
            }
            addOnCanceledListener {
                cont.cancel()
            }
        }
}
