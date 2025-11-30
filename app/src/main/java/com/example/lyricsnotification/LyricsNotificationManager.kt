package com.example.lyricsnotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.palette.graphics.Palette

class LyricsNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "lyrics_channel"
    private val NOTIFICATION_ID = 1337

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Lyrics"
        val descriptionText = "Shows synced lyrics"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showIdleNotification() {
        try {
            // Big View (Expanded)
            val remoteViewsBig = RemoteViews(context.packageName, R.layout.notification_lyrics)
            remoteViewsBig.setTextViewText(R.id.text_lyrics_current, "Waiting for music...")
            remoteViewsBig.setTextViewText(R.id.text_lyrics_previous, "")
            remoteViewsBig.setTextViewText(R.id.text_lyrics_next, "")
            remoteViewsBig.setImageViewResource(R.id.img_background, 0)
            remoteViewsBig.setInt(R.id.layout_root, "setBackgroundColor", Color.BLACK)
            remoteViewsBig.setTextColor(R.id.text_lyrics_current, Color.WHITE)

            // Small View (Collapsed)
            val remoteViewsSmall = RemoteViews(context.packageName, R.layout.notification_lyrics_small)
            remoteViewsSmall.setTextViewText(R.id.text_lyrics_current, "Waiting for music...")
            remoteViewsSmall.setImageViewResource(R.id.img_background, 0)
            remoteViewsSmall.setInt(R.id.layout_root, "setBackgroundColor", Color.BLACK)
            remoteViewsSmall.setTextColor(R.id.text_lyrics_current, Color.WHITE)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setCustomBigContentView(remoteViewsBig)
                .setCustomContentView(remoteViewsSmall)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setColorized(true)
                .setColor(Color.BLACK)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateNotification(
        currentLine: String,
        prevLine: String,
        nextLine: String,
        albumArt: Bitmap?,
        isPlaying: Boolean
    ) {
        try {
            // Initialize Builder FIRST so we can modify it later
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying)
                .setColorized(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Big View
            val remoteViewsBig = RemoteViews(context.packageName, R.layout.notification_lyrics)
            remoteViewsBig.setTextViewText(R.id.text_lyrics_current, currentLine)
            remoteViewsBig.setTextViewText(R.id.text_lyrics_previous, prevLine)
            remoteViewsBig.setTextViewText(R.id.text_lyrics_next, nextLine)
            
            // Small View
            val remoteViewsSmall = RemoteViews(context.packageName, R.layout.notification_lyrics_small)
            remoteViewsSmall.setTextViewText(R.id.text_lyrics_current, currentLine)

            if (albumArt != null) {
                try {
                    val scaled = Bitmap.createScaledBitmap(albumArt, 20, 20, true)
                    
                    // Generate Palette for dynamic text color
                    val palette = Palette.from(albumArt).generate()
                    val dynamicColor = palette.getLightVibrantColor(Color.WHITE)
                    val backgroundColor = palette.getDominantColor(Color.BLACK)
                    
                    // Set to Big
                    remoteViewsBig.setImageViewBitmap(R.id.img_background, scaled)
                    remoteViewsBig.setTextColor(R.id.text_lyrics_current, dynamicColor)
                    remoteViewsBig.setTextColor(R.id.text_lyrics_previous, setAlpha(dynamicColor, 170))
                    remoteViewsBig.setTextColor(R.id.text_lyrics_next, setAlpha(dynamicColor, 170))
                    
                    // Set to Small
                    remoteViewsSmall.setImageViewBitmap(R.id.img_background, scaled)
                    remoteViewsSmall.setTextColor(R.id.text_lyrics_current, dynamicColor)
                    
                    // Set Background Color dynamically
                    builder.setColor(backgroundColor)
                    
                } catch (e: Exception) {
                    // Fallback
                    remoteViewsBig.setImageViewResource(R.id.img_background, 0)
                    remoteViewsBig.setInt(R.id.layout_root, "setBackgroundColor", Color.BLACK)
                    remoteViewsBig.setTextColor(R.id.text_lyrics_current, Color.WHITE)
                    
                    remoteViewsSmall.setImageViewResource(R.id.img_background, 0)
                    remoteViewsSmall.setInt(R.id.layout_root, "setBackgroundColor", Color.BLACK)
                    remoteViewsSmall.setTextColor(R.id.text_lyrics_current, Color.WHITE)
                    
                    builder.setColor(Color.BLACK)
                }
            } else {
                 remoteViewsBig.setImageViewResource(R.id.img_background, 0)
                 remoteViewsBig.setInt(R.id.layout_root, "setBackgroundColor", Color.BLACK)
                 remoteViewsBig.setTextColor(R.id.text_lyrics_current, Color.WHITE)
                 
                 remoteViewsSmall.setImageViewResource(R.id.img_background, 0)
                 remoteViewsSmall.setInt(R.id.layout_root, "setBackgroundColor", Color.BLACK)
                 remoteViewsSmall.setTextColor(R.id.text_lyrics_current, Color.WHITE)
                 
                 builder.setColor(Color.BLACK)
            }

            // Attach views to builder
            builder.setCustomBigContentView(remoteViewsBig)
            builder.setCustomContentView(remoteViewsSmall)

            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun setAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
