package com.example.myradio.data.repository

import android.content.Context
import android.util.Log
import com.example.myradio.data.local.StationsDataSource
import com.example.myradio.data.local.db.AppSettingEntity
import com.example.myradio.data.local.db.DeletedDefaultEntity
import com.example.myradio.data.local.db.MyRadioDatabase
import com.example.myradio.data.local.db.StationPreferencesEntity
import com.example.myradio.data.local.db.toRadioStation
import com.example.myradio.data.model.RadioStation
import com.example.myradio.data.model.StationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class RadioRepository(
    private val context: Context,
    private val database: MyRadioDatabase,
    scope: CoroutineScope
) {

    companion object {
        private const val TAG = "RadioRepository"
    }

    private val stationDao = database.stationDao()
    private val settingsDao = database.appSettingsDao()

    val stations: StateFlow<List<RadioStation>> = combine(
        stationDao.observeUserStations(),
        stationDao.observePreferences(),
        stationDao.observeDeletedDefaults()
    ) { userStationEntities, prefEntities, deletedDefaultIds ->
        val defaultStations = StationsDataSource.getStations()
        val deletedSet = deletedDefaultIds.toSet()
        val prefsMap = prefEntities.associate { it.stationId to StationPreferences(it.isFavorite, it.sortOrder) }
        val userStations = userStationEntities.map { it.toRadioStation() }

        val activeDefaults = defaultStations.filter { it.id !in deletedSet }
        val allStations = (activeDefaults + userStations).map { station ->
            val pref = prefsMap[station.id]
            if (pref != null) {
                station.copy(isFavorite = pref.isFavorite, sortOrder = pref.sortOrder)
            } else {
                station
            }
        }

        allStations.sortedWith(
            compareByDescending<RadioStation> { it.isFavorite }
                .thenBy { it.sortOrder }
                .thenBy { it.id }
        ).also {
            Log.d(TAG, "stations: ${activeDefaults.size} defaults + ${userStations.size} user = ${it.size} total")
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun getStationById(id: Int): RadioStation? {
        return stations.value.find { it.id == id }
    }

    suspend fun addStation(name: String, streamUrl: String, genre: String, country: String, logoUrl: String = "") {
        val newId = generateNextId()
        val entity = com.example.myradio.data.local.db.UserStationEntity(
            id = newId,
            name = name,
            streamUrl = streamUrl,
            genre = genre,
            country = country,
            logoUrl = logoUrl
        )
        stationDao.insertStation(entity)
        Log.d(TAG, "addStation: $name (id=$newId)")
    }

    suspend fun deleteStation(id: Int) {
        val defaultIds = StationsDataSource.getStations().map { it.id }.toSet()
        if (id in defaultIds) {
            stationDao.insertDeletedDefault(DeletedDefaultEntity(id))
            Log.d(TAG, "deleteStation: default station id=$id marked as deleted")
        } else {
            stationDao.deleteStation(id)
            Log.d(TAG, "deleteStation: user station id=$id removed")
        }
    }

    suspend fun toggleFavorite(stationId: Int) {
        val current = stations.value.find { it.id == stationId }
        val currentFav = current?.isFavorite ?: false
        val currentOrder = current?.sortOrder ?: Int.MAX_VALUE
        stationDao.upsertPreference(
            StationPreferencesEntity(
                stationId = stationId,
                isFavorite = !currentFav,
                sortOrder = currentOrder
            )
        )
    }

    suspend fun updateStationOrder(reorderedStations: List<RadioStation>) {
        reorderedStations.forEachIndexed { index, station ->
            stationDao.upsertPreference(
                StationPreferencesEntity(
                    stationId = station.id,
                    isFavorite = station.isFavorite,
                    sortOrder = index
                )
            )
        }
    }

    suspend fun getLastPlayedStationId(): Int? {
        return settingsDao.getValue("last_played_station_id")?.toIntOrNull()
    }

    suspend fun saveLastPlayedStationId(stationId: Int) {
        settingsDao.upsert(AppSettingEntity("last_played_station_id", stationId.toString()))
    }

    private suspend fun generateNextId(): Int {
        val defaultMax = StationsDataSource.getStations().maxOfOrNull { it.id } ?: 0
        val userMax = stationDao.getMaxUserStationId() ?: 0
        return maxOf(defaultMax, userMax) + 1
    }
}
