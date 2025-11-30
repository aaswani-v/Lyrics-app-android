package com.example.lyricsnotification

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class SyncedLine(
    val timestamp: Long, // in milliseconds
    val content: String
)

data class LrcLibResponse(
    val id: Int,
    val name: String,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val duration: Double,
    val instrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

interface LrcLibApi {
    @GET("search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): List<LrcLibResponse>
}

object LyricsRepository {
    private const val BASE_URL = "https://lrclib.net/api/"

    private val api: LrcLibApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrcLibApi::class.java)
    }

    suspend fun fetchLyrics(track: String, artist: String, album: String, durationSec: Double): List<SyncedLine>? {
        val query = "$track $artist"
        android.util.Log.d("LyricsRepo", "Searching for: $query, duration: $durationSec")
        
        return try {
            val results = api.searchLyrics(query)
            
            // Find the best match
            val bestMatch = results.find { 
                // Allow 2 seconds difference in duration
                val durationDiff = kotlin.math.abs(it.duration - durationSec)
                durationDiff < 2.0
            } ?: results.firstOrNull() // Fallback to first result if no duration match

            if (bestMatch?.syncedLyrics != null) {
                android.util.Log.d("LyricsRepo", "Found match: ${bestMatch.trackName}")
                parseLrc(bestMatch.syncedLyrics)
            } else {
                android.util.Log.d("LyricsRepo", "No synced lyrics found")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("LyricsRepo", "Error fetching lyrics: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun parseLrc(lrc: String): List<SyncedLine> {
        val lines = mutableListOf<SyncedLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)")
        
        lrc.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val (min, sec, centi, text) = match.destructured
                val timeMs = (min.toLong() * 60 * 1000) + (sec.toLong() * 1000) + (centi.toLong() * 10)
                lines.add(SyncedLine(timeMs, text.trim()))
            }
        }
        return lines.sortedBy { it.timestamp }
    }
}
