package com.taskify.pro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.taskify.pro.utils.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — entry-point initialisation.
 *
 * Responsibilities:
 *  - Enable Hilt dependency injection via @HiltAndroidApp.
 *  - Create the notification channel at app start so alarms can
 *    display heads-up notifications immediately.
 */
@HiltAndroidApp
class TaskifyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
