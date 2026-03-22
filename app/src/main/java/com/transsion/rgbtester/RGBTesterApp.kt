package com.transsion.rgbtester

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class RGBTesterApp : Application() {

    companion object {
        const val CHANNEL_ID_RGB_TEST = "rgb_test_channel"
        const val CHANNEL_ID_EFFECT = "rgb_effect_channel"
    }

    override fun onCreate() {
        super.onCreate()

        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val testChannel = NotificationChannel(
                CHANNEL_ID_RGB_TEST,
                "RGB Test Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for RGB test progress"
            }

            val effectChannel = NotificationChannel(
                CHANNEL_ID_EFFECT,
                "RGB Effect Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for RGB effect status"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(listOf(testChannel, effectChannel))
        }
    }
}
