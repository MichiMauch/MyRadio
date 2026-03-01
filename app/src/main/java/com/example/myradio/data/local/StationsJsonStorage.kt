package com.example.myradio.data.local

import android.content.Context
import android.util.Log
import com.example.myradio.data.model.RadioStation
import com.example.myradio.data.model.StationPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object StationsJsonStorage {

    private const val TAG = "StationsJsonStorage"
    private const val USER_STATIONS_FILE = "user_stations.json"
    private const val DELETED_DEFAULTS_FILE = "deleted_defaults.json"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // --- User Stations ---

    fun loadUserStations(context: Context): List<RadioStation> {
        val file = File(context.filesDir, USER_STATIONS_FILE)
        if (!file.exists()) return emptyList()

        return try {
            val content = file.readText()
            json.decodeFromString<List<RadioStation>>(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user stations", e)
            emptyList()
        }
    }

    fun saveUserStations(context: Context, stations: List<RadioStation>) {
        try {
            val file = File(context.filesDir, USER_STATIONS_FILE)
            file.writeText(json.encodeToString(stations))
            Log.d(TAG, "Saved ${stations.size} user stations")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user stations", e)
        }
    }

    // --- Last Played Station ---

    private const val LAST_PLAYED_FILE = "last_played_station.json"

    fun loadLastPlayedStationId(context: Context): Int? {
        val file = File(context.filesDir, LAST_PLAYED_FILE)
        if (!file.exists()) return null
        return try {
            json.decodeFromString<Int>(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Error loading last played station", e)
            null
        }
    }

    fun saveLastPlayedStationId(context: Context, stationId: Int) {
        try {
            val file = File(context.filesDir, LAST_PLAYED_FILE)
            file.writeText(json.encodeToString(stationId))
        } catch (e: Exception) {
            Log.e(TAG, "Error saving last played station", e)
        }
    }

    // --- Station Preferences (Favorites & Sort Order) ---

    private const val STATION_PREFS_FILE = "station_preferences.json"

    fun loadStationPreferences(context: Context): Map<Int, StationPreferences> {
        val file = File(context.filesDir, STATION_PREFS_FILE)
        if (!file.exists()) return emptyMap()
        return try {
            json.decodeFromString<Map<Int, StationPreferences>>(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Error loading station preferences", e)
            emptyMap()
        }
    }

    fun saveStationPreferences(context: Context, prefs: Map<Int, StationPreferences>) {
        try {
            val file = File(context.filesDir, STATION_PREFS_FILE)
            file.writeText(json.encodeToString(prefs))
            Log.d(TAG, "Saved preferences for ${prefs.size} stations")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving station preferences", e)
        }
    }

    // --- Deleted Default IDs ---

    fun loadDeletedDefaultIds(context: Context): Set<Int> {
        val file = File(context.filesDir, DELETED_DEFAULTS_FILE)
        if (!file.exists()) return emptySet()

        return try {
            val content = file.readText()
            json.decodeFromString<Set<Int>>(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading deleted defaults", e)
            emptySet()
        }
    }

    fun saveDeletedDefaultIds(context: Context, ids: Set<Int>) {
        try {
            val file = File(context.filesDir, DELETED_DEFAULTS_FILE)
            file.writeText(json.encodeToString(ids))
            Log.d(TAG, "Saved ${ids.size} deleted default IDs")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving deleted defaults", e)
        }
    }
}
