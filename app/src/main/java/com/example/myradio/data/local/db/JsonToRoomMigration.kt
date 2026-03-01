package com.example.myradio.data.local.db

import android.content.Context
import android.util.Log
import com.example.myradio.data.local.PodcastSubscriptionsStorage
import com.example.myradio.data.local.SongHistoryStorage
import com.example.myradio.data.local.StationsJsonStorage
import java.io.File

object JsonToRoomMigration {

    private const val TAG = "JsonToRoomMigration"
    private const val KEY_MIGRATION_DONE = "json_to_room_migration_done"

    suspend fun migrateIfNeeded(context: Context, database: MyRadioDatabase) {
        val settingsDao = database.appSettingsDao()

        val done = settingsDao.getValue(KEY_MIGRATION_DONE)
        if (done == "true") return

        Log.d(TAG, "Starting JSON to Room migration...")

        try {
            migrateUserStations(context, database)
            migrateStationPreferences(context, database)
            migrateDeletedDefaults(context, database)
            migrateLastPlayedStation(context, database)
            migrateSongHistory(context, database)
            migratePodcastSubscriptions(context, database)

            settingsDao.upsert(AppSettingEntity(KEY_MIGRATION_DONE, "true"))

            deleteJsonFiles(context)
            Log.d(TAG, "Migration completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed, will retry on next start", e)
        }
    }

    private suspend fun migrateUserStations(context: Context, database: MyRadioDatabase) {
        val stations = StationsJsonStorage.loadUserStations(context)
        if (stations.isEmpty()) return

        val entities = stations.map { it.toEntity() }
        database.stationDao().insertAllStations(entities)
        Log.d(TAG, "Migrated ${entities.size} user stations")
    }

    private suspend fun migrateStationPreferences(context: Context, database: MyRadioDatabase) {
        val prefs = StationsJsonStorage.loadStationPreferences(context)
        if (prefs.isEmpty()) return

        val entities = prefs.map { (stationId, pref) ->
            StationPreferencesEntity(
                stationId = stationId,
                isFavorite = pref.isFavorite,
                sortOrder = pref.sortOrder
            )
        }
        database.stationDao().insertAllPreferences(entities)
        Log.d(TAG, "Migrated ${entities.size} station preferences")
    }

    private suspend fun migrateDeletedDefaults(context: Context, database: MyRadioDatabase) {
        val deletedIds = StationsJsonStorage.loadDeletedDefaultIds(context)
        if (deletedIds.isEmpty()) return

        val entities = deletedIds.map { DeletedDefaultEntity(it) }
        database.stationDao().insertAllDeletedDefaults(entities)
        Log.d(TAG, "Migrated ${entities.size} deleted default IDs")
    }

    private suspend fun migrateLastPlayedStation(context: Context, database: MyRadioDatabase) {
        val lastPlayedId = StationsJsonStorage.loadLastPlayedStationId(context) ?: return
        database.appSettingsDao().upsert(
            AppSettingEntity("last_played_station_id", lastPlayedId.toString())
        )
        Log.d(TAG, "Migrated last played station ID: $lastPlayedId")
    }

    private suspend fun migrateSongHistory(context: Context, database: MyRadioDatabase) {
        val history = SongHistoryStorage.loadHistory(context)
        if (history.isEmpty()) return

        val entities = history.map { it.toEntity() }
        database.songHistoryDao().insertAll(entities)
        Log.d(TAG, "Migrated ${entities.size} song history entries")
    }

    private suspend fun migratePodcastSubscriptions(context: Context, database: MyRadioDatabase) {
        val subscriptions = PodcastSubscriptionsStorage.load(context)
        if (subscriptions.isEmpty()) return

        val entities = subscriptions.map { it.toEntity() }
        database.podcastSubscriptionDao().insertAll(entities)
        Log.d(TAG, "Migrated ${entities.size} podcast subscriptions")
    }

    private fun deleteJsonFiles(context: Context) {
        val fileNames = listOf(
            "user_stations.json",
            "deleted_defaults.json",
            "last_played_station.json",
            "station_preferences.json",
            "song_history.json",
            "podcast_subscriptions.json"
        )
        fileNames.forEach { name ->
            val file = File(context.filesDir, name)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted $name")
            }
        }
    }
}
