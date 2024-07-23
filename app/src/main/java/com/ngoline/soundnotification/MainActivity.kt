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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
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
                val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                if (uri != null) {
                    sharedPreferences.edit().putString("SelectedSoundUri", uri.toString()).apply()
                }
            }
        }

        setContent {
            SoundNotificationTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Greeting() { launchRingtonePicker() }
                        Spacer(modifier = Modifier.height(16.dp))
                        VolumeSlider(sharedPreferences)
                        Spacer(modifier = Modifier.height(16.dp))
                        KeywordListManager(sharedPreferences)
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
    fun Greeting(onButtonClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.elevatedCardElevation()
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
    }

    @Composable
    fun VolumeSlider(sharedPreferences: SharedPreferences) {
        var volume by remember { mutableStateOf(sharedPreferences.getFloat("selectedVolume", 1.0f)) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.elevatedCardElevation()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Select Volume")
                Slider(
                    value = volume,
                    onValueChange = { newVolume: Float ->
                        volume = newVolume
                        sharedPreferences.edit().putFloat("selectedVolume", newVolume).apply()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Text(text = "Volume: ${(volume * 100).toInt()}%")
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    @Composable
    fun KeywordListManager(sharedPreferences: SharedPreferences) {
        var keywordText by remember { mutableStateOf("") }
        val keywordList = remember { mutableStateListOf<String>() }
        val keyboardController = LocalSoftwareKeyboardController.current

        fun addKeyword() {
            if (keywordText.isNotBlank()) {
                keywordList.add(keywordText)
                sharedPreferences
                    .edit()
                    .putStringSet("keywords", keywordList.toSet())
                    .apply()
                keywordText = ""
            }
        }

        LaunchedEffect(Unit) {
            val savedKeywords = sharedPreferences.getStringSet("keywords", emptySet())
            keywordList.addAll(savedKeywords ?: emptySet())
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.elevatedCardElevation()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text="Manage Keywords")
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = keywordText,
                        onValueChange = { newText: String -> keywordText = newText },
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        label = { Text("Add keywords here...") },
                        textStyle = LocalTextStyle.current,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            addKeyword()
                            keyboardController?.hide()
                        })
                    )
                    Button(onClick = { addKeyword() }) {
                        Text("Add")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(keywordList.size) { index ->
                        val keyword = keywordList[index]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = keyword, modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        keywordList.remove(keyword)
                                        sharedPreferences
                                            .edit()
                                            .putStringSet("keywords", keywordList.toSet())
                                            .apply()
                                    }
                                    .padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun CombinedPreview() {
        SoundNotificationTheme {
            // Use a mock shared preferences for preview
            val mockSharedPreferences = createMockSharedPreferences().apply {
                edit().putStringSet("keywords", setOf("example1", "example2")).apply()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Greeting(onButtonClick = {})
                Spacer(modifier = Modifier.height(16.dp))
                VolumeSlider(mockSharedPreferences)
                Spacer(modifier = Modifier.height(16.dp))
                KeywordListManager(mockSharedPreferences)
            }
        }
    }

    private fun createMockSharedPreferences(): SharedPreferences {
        return object : SharedPreferences {
            private val data = mutableMapOf<String, Any?>()

            override fun contains(key: String?): Boolean = data.containsKey(key)
            override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
            override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
            override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
            override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
            override fun getAll(): MutableMap<String, *> = mutableMapOf<String, Any?>()

            override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
            @Suppress("UNCHECKED_CAST")
            override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = data[key] as? MutableSet<String> ?: defValues

            override fun edit(): SharedPreferences.Editor = MockEditor(data)
            override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {

            }

            override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {

            }

            private inner class MockEditor(private val data: MutableMap<String, Any?>) : SharedPreferences.Editor {
                private val changes = mutableMapOf<String, Any?>()
                override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { changes[key!!] = value }
                override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { changes[key!!] = values }
                override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { changes[key!!] = value }
                override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { changes[key!!] = value }
                override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { changes[key!!] = value }
                override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { changes[key!!] = value }
                override fun remove(key: String?): SharedPreferences.Editor = apply { changes.remove(key) }
                override fun clear(): SharedPreferences.Editor = apply { changes.clear() }
                override fun commit(): Boolean = applyChangesBool()
                override fun apply() = applyChanges()
                private fun applyChangesBool(): Boolean {
                    data.putAll(changes)
                    changes.clear()
                    return true
                }
                private fun applyChanges() {
                    data.putAll(changes)
                    changes.clear()
                }
            }
        }
    }
}