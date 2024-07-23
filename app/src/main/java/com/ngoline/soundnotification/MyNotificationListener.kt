package com.ngoline.soundnotification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.util.Log

class MyNotificationListener :NotificationListenerService() {

    private val notificationsToSkip = listOf(
        "android.app.Notification",
        "androidx.core.app.NotificationCompat"
    )

    override fun onListenerConnected() {
        Log.d("NotificationListener", "Service Connected")
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d("NotificationListener", "Notification from ${sbn.packageName}")
        val notificationText = extractNotificationText(sbn.notification)

        val sharedPrefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val keywords = sharedPrefs.getStringSet("keywords", emptySet())
        if (keywords != null && keywords.any { notificationText.contains(it, ignoreCase = true) }) {
            Log.d("NotificationListener", "Will play a sound!")
            playSound()
        } else {
            Log.d("NotificationListener", "Will NOT play a sound for $notificationText")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional: Handle removed notifications if necessary
        val notificationText = extractNotificationText(sbn.notification)
        Log.d("NotificationListener", "Notification removed from ${sbn.packageName}: $notificationText")
    }

    private fun extractNotificationText(notification: Notification): String {
        val extras = notification.extras
        val textComponents = mutableListOf<String>()

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (!text.isNullOrBlank()) {
            Log.d("NotificationListener", "text was $text")
            textComponents.add(text)
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        if (!title.isNullOrBlank()) {
            Log.d("NotificationListener", "title was $title")
            textComponents.add(title)
        }

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        if (!subText.isNullOrBlank()) {
            Log.d("NotificationListener", "subText was $subText")
            textComponents.add(subText)
        }

        // Check for other possible text fields
        for (key in extras.keySet()) {
            try {
                val value = extras.getCharSequence(key)?.toString()
                if (value is CharSequence
                    && value.isNotBlank()
                    && !textComponents.contains(value.toString())
                ) {
                    var shouldSkip = false
                    notificationsToSkip.forEach {
                        if (value.toString().contains(it)) {
                            shouldSkip = true
                        }
                    }
                    if (!shouldSkip) {
                        Log.d("NotificationListener", "$key was $value")
                        textComponents.add(value)
                    }
                }
            } catch (e: Exception) {
                // Do nothing
            }
        }

        return textComponents.joinToString(" ")
    }

    private fun playSound() {
        Log.d("NotificationListener", "Playing sound")
        val sharedPrefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val uriString = sharedPrefs.getString("SelectedSoundUri", null)
        val selectedVolume = sharedPrefs.getFloat("selectedVolume", 1.0f)
        uriString?.let { itString ->
            val uri = Uri.parse(itString)
            uri?.let {
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val newVolume = (maxVolume * selectedVolume).toInt()

                // Set the volume to the selected level
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0)

                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)  // or USAGE_NOTIFICATION_RINGTONE
                            .build()
                    )
                    setDataSource(applicationContext, it)
                    prepare()  // might take time for larger files
                    setOnCompletionListener {
                        // Restore the original volume
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume,0)
                        it.release()
                    }
                    start()
                }
            }
        }
    }
}