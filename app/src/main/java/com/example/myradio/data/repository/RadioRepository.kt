package com.example.myradio.data.repository

import android.content.Context
import android.util.Log
import com.example.myradio.data.local.StationsDataSource
import com.example.myradio.data.local.StationsJsonStorage
import com.example.myradio.data.model.RadioStation
import com.example.myradio.data.model.StationPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RadioRepository(private val context: Context) {

    companion object {
        private const val TAG = "RadioRepository"
    }

    private val _stations = MutableStateFlow<List<RadioStation>>(emptyList())
    val stations: StateFlow<List<RadioStation>> = _stations.asStateFlow()

    init {
        refreshStations()
    }

    private fun refreshStations() {
        val defaultStations = StationsDataSource.getStations()
        val deletedDefaultIds = StationsJsonStorage.loadDeletedDefaultIds(context)
        val userStations = StationsJsonStorage.loadUserStations(context)
        val prefs = StationsJsonStorage.loadStationPreferences(context)

        val activeDefaults = defaultStations.filter { it.id !in deletedDefaultIds }
        val allStations = (activeDefaults + userStations).map { station ->
            val pref = prefs[station.id]
            if (pref != null) {
                station.copy(isFavorite = pref.isFavorite, sortOrder = pref.sortOrder)
            } else {
                station
            }
        }

        // Sort: favorites first, then by sortOrder, then by id
        _stations.value = allStations.sortedWith(
            compareByDescending<RadioStation> { it.isFavorite }
                .thenBy { it.sortOrder }
                .thenBy { it.id }
        )
        Log.d(TAG, "refreshStations: ${activeDefaults.size} defaults + ${userStations.size} user = ${allStations.size} total")
    }

    fun getAllStations(): List<RadioStation> {
        return _stations.value
    }

    fun getStationById(id: Int): RadioStation? {
        return _stations.value.find { it.id == id }
    }

    fun addStation(name: String, streamUrl: String, genre: String, country: String, logoUrl: String = "") {
        val newId = generateNextId()
        val station = RadioStation(
            id = newId,
            name = name,
            streamUrl = streamUrl,
            genre = genre,
            country = country,
            logoUrl = logoUrl
        )

        val userStations = StationsJsonStorage.loadUserStations(context).toMutableList()
        userStations.add(station)
        StationsJsonStorage.saveUserStations(context, userStations)

        refreshStations()
        Log.d(TAG, "addStation: ${station.name} (id=$newId)")
    }

    fun deleteStation(id: Int) {
        val defaultIds = StationsDataSource.getStations().map { it.id }.toSet()

        if (id in defaultIds) {
            val deletedIds = StationsJsonStorage.loadDeletedDefaultIds(context).toMutableSet()
            deletedIds.add(id)
            StationsJsonStorage.saveDeletedDefaultIds(context, deletedIds)
            Log.d(TAG, "deleteStation: default station id=$id marked as deleted")
        } else {
            val userStations = StationsJsonStorage.loadUserStations(context).toMutableList()
            userStations.removeAll { it.id == id }
            StationsJsonStorage.saveUserStations(context, userStations)
            Log.d(TAG, "deleteStation: user station id=$id removed")
        }

        refreshStations()
    }

    // --- Favorites & Sorting ---

    fun toggleFavorite(stationId: Int) {
        val prefs = StationsJsonStorage.loadStationPreferences(context).toMutableMap()
        val current = prefs[stationId] ?: StationPreferences()
        prefs[stationId] = current.copy(isFavorite = !current.isFavorite)
        StationsJsonStorage.saveStationPreferences(context, prefs)
        refreshStations()
    }

    fun updateStationOrder(reorderedStations: List<RadioStation>) {
        val prefs = StationsJsonStorage.loadStationPreferences(context).toMutableMap()
        reorderedStations.forEachIndexed { index, station ->
            val current = prefs[station.id] ?: StationPreferences()
            prefs[station.id] = current.copy(sortOrder = index)
        }
        StationsJsonStorage.saveStationPreferences(context, prefs)
        refreshStations()
    }

    // --- Last Played Station ---

    fun getLastPlayedStationId(): Int? =
        StationsJsonStorage.loadLastPlayedStationId(context)

    fun saveLastPlayedStationId(stationId: Int) {
        StationsJsonStorage.saveLastPlayedStationId(context, stationId)
    }

    private fun generateNextId(): Int {
        val defaultMax = StationsDataSource.getStations().maxOfOrNull { it.id } ?: 0
        val userMax = StationsJsonStorage.loadUserStations(context).maxOfOrNull { it.id } ?: 0
        return maxOf(defaultMax, userMax) + 1
    }
}
