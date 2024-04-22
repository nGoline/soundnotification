package com.ngoline.soundnotification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.net.Uri
import android.util.Log

class MyNotificationListener :NotificationListenerService() {

    override fun onListenerConnected() {
        Log.d("NotificationListener", "Service Connected")
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d("NotificationListener", "Notification from ${sbn.packageName}")
        playSound()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional: Handle removed notifications if necessary
        Log.d("NotificationListener", "Notification removed from ${sbn.packageName}")
    }

    private fun playSound() {
        Log.d("NotificationListener", "Playing sound")
        val sharedPrefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val uriString = sharedPrefs.getString("SelectedSoundUri", null)
        uriString?.let { itString ->
            val uri = Uri.parse(itString)
            uri?.let {
                val mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)  // or USAGE_NOTIFICATION_RINGTONE
                            .build()
                    )
                    setDataSource(applicationContext, it)
                    prepare()  // might take time for larger files
                    setOnCompletionListener {
                        it.release()
                    }
                    start()
                }
            }
        }
    }
}