package com.example.myradio.data.local

import android.content.Context
import android.util.Log
import com.example.myradio.data.model.PlayedSong
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object SongHistoryStorage {

    private const val TAG = "SongHistoryStorage"
    private const val HISTORY_FILE = "song_history.json"
    private const val MAX_HISTORY_ENTRIES = 500

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun loadHistory(context: Context): List<PlayedSong> {
        val file = File(context.filesDir, HISTORY_FILE)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<PlayedSong>>(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Error loading history", e)
            emptyList()
        }
    }

    @Synchronized
    fun addEntry(context: Context, entry: PlayedSong) {
        val history = loadHistory(context).toMutableList()
        history.add(0, entry) // newest first
        if (history.size > MAX_HISTORY_ENTRIES) {
            history.subList(MAX_HISTORY_ENTRIES, history.size).clear()
        }
        saveHistory(context, history)
    }

    fun clearHistory(context: Context) {
        saveHistory(context, emptyList())
    }

    private fun saveHistory(context: Context, history: List<PlayedSong>) {
        try {
            val file = File(context.filesDir, HISTORY_FILE)
            file.writeText(json.encodeToString(history))
        } catch (e: Exception) {
            Log.e(TAG, "Error saving history", e)
        }
    }
}
