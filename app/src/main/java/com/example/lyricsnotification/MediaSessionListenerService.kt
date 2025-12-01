package com.example.lyricsnotification

import android.graphics.Bitmap // Added import
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.*

class MediaSessionListenerService : NotificationListenerService(), MediaSessionManager.OnActiveSessionsChangedListener {

    private var currentController: MediaController? = null
    private var lyricsManager: LyricsNotificationManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    private var currentLyrics: List<SyncedLine>? = null
    private var lastTrackId: String = ""
    private var isPlaying = false
    private var isServiceEnabled = true
    private val PREFS_NAME = "LyricsPrefs"
    private val KEY_SERVICE_ENABLED = "service_enabled"

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.lyricsnotification.TOGGLE_SERVICE") {
                val enabled = intent.getBooleanExtra("enabled", true)
                isServiceEnabled = enabled
                if (enabled) {
                    // Restart logic
                    val mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
                    val componentName = ComponentName(this@MediaSessionListenerService, MediaSessionListenerService::class.java)
                    onActiveSessionsChanged(mediaSessionManager.getActiveSessions(componentName))
                } else {
                    // Stop logic
                    updateJob?.cancel()
                    lyricsManager?.cancelNotification()
                    currentController?.unregisterCallback(callback)
                    currentController = null
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        lyricsManager = LyricsNotificationManager(this)
        Log.d("LyricsService", "Service Created")
        
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isServiceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, true)

        val filter = IntentFilter("com.example.lyricsnotification.TOGGLE_SERVICE")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(toggleReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(toggleReceiver, filter)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("LyricsService", "Listener Connected")
        val mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, MediaSessionListenerService::class.java)
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(this, componentName)
            // Initial check
            onActiveSessionsChanged(mediaSessionManager.getActiveSessions(componentName))
        } catch (e: SecurityException) {
            Log.e("LyricsService", "No permission to access media sessions")
        }
    }

    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        Log.d("LyricsService", "Active Sessions Changed")
        findActiveMediaSession(controllers)
    }

    private fun findActiveMediaSession(controllers: List<MediaController>?) {
        if (!isServiceEnabled) return

        if (controllers.isNullOrEmpty()) {
            lyricsManager?.showIdleNotification()
            return
        }

        // Pick the first active one or one that is playing
        val controller = controllers.firstOrNull { 
            val state = it.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull()

        if (controller != null && controller != currentController) {
            registerCallback(controller)
        }
    }

    private fun registerCallback(controller: MediaController) {
        currentController?.unregisterCallback(callback)
        currentController = controller
        currentController?.registerCallback(callback)
        
        // Initial update
        updateMetadata(controller.metadata)
        updatePlaybackState(controller.playbackState)
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMetadata(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updatePlaybackState(state)
        }
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        if (metadata == null) return
        
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)

        val trackId = "$title-$artist"
        if (trackId != lastTrackId) {
            lastTrackId = trackId
            currentLyrics = null // Reset lyrics
            
            // Fetch new lyrics
            serviceScope.launch(Dispatchers.IO) {
                val lyrics = LyricsRepository.fetchLyrics(title, artist, album, duration / 1000.0)
                withContext(Dispatchers.Main) {
                    currentLyrics = lyrics
                    // Trigger an immediate update if playing
                    if (isPlaying) {
                        startLyricsUpdater(art)
                    }
                }
            }
        }
    }

    private var updateJob: Job? = null

    private fun updatePlaybackState(state: PlaybackState?) {
        if (state == null) return
        
        isPlaying = state.state == PlaybackState.STATE_PLAYING
        
        if (isPlaying) {
            val metadata = currentController?.metadata
            val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            startLyricsUpdater(art)
        } else {
            updateJob?.cancel()
            lyricsManager?.updateNotification("Paused", "", "", null, false)
        }
    }

    private fun startLyricsUpdater(art: Bitmap?) {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive && isPlaying) {
                val state = currentController?.playbackState ?: break
                val position = state.position
                val lastUpdateTime = state.lastPositionUpdateTime
                val timeDiff = android.os.SystemClock.elapsedRealtime() - lastUpdateTime
                val currentPos = position + (timeDiff * state.playbackSpeed).toLong()

                updateLyricsForPosition(currentPos, art)
                
                delay(500) // Update every 500ms
            }
        }
    }

    private fun updateLyricsForPosition(position: Long, art: Bitmap?) {
        val lyrics = currentLyrics
        if (lyrics.isNullOrEmpty()) {
            lyricsManager?.updateNotification("No Lyrics Found", "", "", art, true)
            return
        }

        // Find current line
        // We want the last line where timestamp <= currentPos
        val index = lyrics.indexOfLast { it.timestamp <= position }
        
        if (index != -1) {
            val currentLine = lyrics[index].content
            val prevLine = if (index > 0) lyrics[index - 1].content else ""
            val nextLine = if (index < lyrics.size - 1) lyrics[index + 1].content else ""
            
            lyricsManager?.updateNotification(currentLine, prevLine, nextLine, art, true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        currentController?.unregisterCallback(callback)
        unregisterReceiver(toggleReceiver)
    }
}
