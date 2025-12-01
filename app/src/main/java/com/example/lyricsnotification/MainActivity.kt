package com.example.lyricsnotification

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var toggleServiceButton: Button
    private val PREFS_NAME = "LyricsPrefs"
    private val KEY_SERVICE_ENABLED = "service_enabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        permissionButton = findViewById(R.id.permission_button)

        permissionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.test_notification_button).setOnClickListener {
            val manager = LyricsNotificationManager(this)
            manager.showIdleNotification()
            android.widget.Toast.makeText(this, "Test Notification Sent", android.widget.Toast.LENGTH_SHORT).show()
        }

        toggleServiceButton = findViewById(R.id.toggle_service_button)
        updateToggleButton()

        toggleServiceButton.setOnClickListener {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val currentState = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
            val newState = !currentState
            
            prefs.edit().putBoolean(KEY_SERVICE_ENABLED, newState).apply()
            updateToggleButton()
            
            val intent = Intent("com.example.lyricsnotification.TOGGLE_SERVICE")
            intent.putExtra("enabled", newState)
            sendBroadcast(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    private fun checkPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        if (isNotificationServiceEnabled()) {
            statusText.text = getString(R.string.permission_granted)
            permissionButton.isEnabled = false
            permissionButton.text = "Permission Granted"
        } else {
            statusText.text = getString(R.string.permission_explanation)
            permissionButton.isEnabled = true
            permissionButton.text = getString(R.string.grant_permission)
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val cn = ComponentName(this, MediaSessionListenerService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    private fun updateToggleButton() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        toggleServiceButton.text = if (isEnabled) "Stop Service" else "Start Service"
    }
}
