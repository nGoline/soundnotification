package com.ngoline.soundnotification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ngoline.soundnotification.ui.theme.SoundNotificationTheme

class MainActivity : ComponentActivity() {
    private lateinit var ringtonePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        if (isNotificationServiceEnabled(this)) {
            // Notification listener service is enabled
            Log.d("MainActivity", "Notification Service is enabled.")
        } else {
            // Notification listener service is not enabled
            Log.d("MainActivity", "Notification Service is not enabled.")
            launchDeviceNotificationOptions()
        }

        // Initialize the launcher
        ringtonePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (uri != null) {
                    sharedPreferences.edit().putString("SelectedSoundUri", uri.toString()).apply()
                }
            }
        }

        setContent {
            SoundNotificationTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Greeting() {
                            launchRingtonePicker()
                        }
                    }
                }
            }
        }
    }

    private fun launchDeviceNotificationOptions() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivity(intent)
    }

    private fun launchRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound")

            // Retrieve the previously selected ringtone URI and set it in the picker
            val existingUriString = sharedPreferences.getString("SelectedSoundUri", null)
            if (!existingUriString.isNullOrEmpty()) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingUriString))
            } else {
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            }
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun isNotificationServiceEnabled(context: Context): Boolean {
        val pkgName = context.packageName
        val flat =
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (flat != null && flat.isNotEmpty()) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) {
                    return true
                }
            }
        }
        return false
    }

    @Composable
    fun Greeting(modifier: Modifier = Modifier, onButtonClick: () -> Unit) {
        Column(modifier = modifier) {
            Text(
                text = "Force notification sound to selected apps"
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onButtonClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Select Sound")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { launchDeviceNotificationOptions() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Change notification options")
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        SoundNotificationTheme {
            Greeting() {}
        }
    }
}