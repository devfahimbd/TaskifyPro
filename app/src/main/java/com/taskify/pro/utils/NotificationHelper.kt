package com.taskify.pro.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.taskify.pro.R
import com.taskify.pro.receiver.NotificationReceiver
import java.util.Calendar
import java.util.Date

/**
 * Handles everything related to local notifications:
 *  - Channel creation (Android 8+)
 *  - Scheduling alarms via AlarmManager
 *  - Cancelling / rescheduling alarms
 *  - Showing immediate notifications
 *
 * On Android 13+ the POST_NOTIFICATIONS run-time permission must be granted
 * by the hosting Activity before any notification can be displayed.
 */
object NotificationHelper {

    // ── Channel ────────────────────────────────────────────────────────────
    const val CHANNEL_ID = "task_reminder_channel"
    private const val CHANNEL_NAME = "Task Reminders"
    private const val CHANNEL_DESC = "Notifications for task reminders"

    // ── Request codes ─────────────────────────────────────────────────────
    private const val REQUEST_CODE_BASE = 1000
    private const val EXTRA_TASK_TITLE = "extra_task_title"
    private const val EXTRA_TASK_ID = "extra_task_id"
    private const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    // ── Notification IDs ──────────────────────────────────────────────────
    private const val NOTIFICATION_ID_BASE = 2000

    // -----------------------------------------------------------------------
    //  Public API
    // -----------------------------------------------------------------------

    /**
     * Create the notification channel. Must be called once, e.g. in Application.onCreate().
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            NotificationManagerCompat.from(context)
                .createNotificationChannel(channel)
        }
    }

    /**
     * Schedule an exact alarm that fires a [NotificationReceiver] at the given [triggerAt].
     *
     * @param taskId       Firestore document ID used as the unique alarm tag.
     * @param title        Task title shown in the notification.
     * @param triggerAt    The exact date/time the notification should appear.
     */
    fun scheduleTaskReminder(
        context: Context,
        taskId: String,
        title: String,
        triggerAt: Date
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Generate a stable request code from the task ID hash so re-scheduling
        // replaces the old alarm instead of stacking duplicates.
        val requestCode = generateRequestCode(taskId)

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_NOTIFICATION_ID, generateNotificationId(taskId))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setExactAndAllowWhileIdle so the alarm fires even in Doze mode.
        // Requires SCHEDULE_EXACT_ALARM permission on Android 12+.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt.time,
            pendingIntent
        )
    }

    /**
     * Cancel a previously-scheduled reminder for a given [taskId].
     */
    fun cancelTaskReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = generateRequestCode(taskId)

        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        // Also dismiss any visible notification for this task.
        NotificationManagerCompat.from(context)
            .cancel(generateNotificationId(taskId))
    }

    /**
     * Show a notification immediately. Called from [NotificationReceiver].
     */
    fun showNotification(context: Context, taskId: String, title: String, notificationId: Int) {
        // Safety-check the POST_NOTIFICATIONS permission on Android 13+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return // Silently skip — permission not granted.
            }
        }

        // Tap on notification opens MainActivity.
        val openIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task Reminder")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /**
     * Reschedule all pending (non-completed) tasks after a device reboot.
     * Call from [com.taskify.pro.receiver.BootReceiver].
     */
    fun rescheduleAllAlarms(context: Context) {
        // The actual rescheduling logic lives in BootReceiver which reads tasks
        // from Firestore and calls scheduleTaskReminder for each pending task.
        // This method exists as a central entry-point.
    }

    // -----------------------------------------------------------------------
    //  Helpers
    // -----------------------------------------------------------------------

    private fun generateRequestCode(taskId: String): Int {
        // Use absolute value to guarantee a positive int.
        return REQUEST_CODE_BASE + (taskId.hashCode() and 0x7FFFFFFF) % 9000
    }

    fun generateNotificationId(taskId: String): Int {
        return NOTIFICATION_ID_BASE + (taskId.hashCode() and 0x7FFFFFFF) % 9000
    }
}
