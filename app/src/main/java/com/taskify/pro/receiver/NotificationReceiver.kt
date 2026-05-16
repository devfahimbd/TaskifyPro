package com.taskify.pro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskify.pro.utils.NotificationHelper

/**
 * BroadcastReceiver that actually displays the notification.
 *
 * Fired by the [android.app.AlarmManager] at the scheduled reminder time.
 * The intent extras carry the task title and notification ID so the
 * notification can be personalised.
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        NotificationHelper.showNotification(
            context = context,
            taskId = taskId,
            title = title,
            notificationId = notificationId
        )
    }
}
