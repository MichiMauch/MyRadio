package com.example.myradio.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {

    @Query("SELECT * FROM stations")
    fun observeUserStations(): Flow<List<UserStationEntity>>

    @Query("SELECT * FROM station_preferences")
    fun observePreferences(): Flow<List<StationPreferencesEntity>>

    @Query("SELECT stationId FROM deleted_defaults")
    fun observeDeletedDefaults(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: UserStationEntity)

    @Query("DELETE FROM stations WHERE id = :id")
    suspend fun deleteStation(id: Int)

    @Upsert
    suspend fun upsertPreference(pref: StationPreferencesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedDefault(entity: DeletedDefaultEntity)

    @Query("DELETE FROM deleted_defaults WHERE stationId = :stationId")
    suspend fun removeDeletedDefault(stationId: Int)

    @Query("SELECT MAX(id) FROM stations")
    suspend fun getMaxUserStationId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStations(stations: List<UserStationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPreferences(prefs: List<StationPreferencesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDeletedDefaults(entities: List<DeletedDefaultEntity>)
}

@Dao
interface SongHistoryDao {

    @Insert
    suspend fun insert(entry: SongHistoryEntity)

    @Query("SELECT * FROM song_history ORDER BY timestamp DESC LIMIT 500")
    fun observeHistory(): Flow<List<SongHistoryEntity>>

    @Query("DELETE FROM song_history")
    suspend fun clearAll()

    @Query("DELETE FROM song_history WHERE id NOT IN (SELECT id FROM song_history ORDER BY timestamp DESC LIMIT 500)")
    suspend fun trimToMaxEntries()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<SongHistoryEntity>)
}

@Dao
interface PodcastSubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: PodcastSubscriptionEntity)

    @Query("DELETE FROM podcast_subscriptions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM podcast_subscriptions ORDER BY id DESC")
    fun observeSubscriptions(): Flow<List<PodcastSubscriptionEntity>>

    @Query("SELECT * FROM podcast_subscriptions ORDER BY id DESC")
    suspend fun getAll(): List<PodcastSubscriptionEntity>

    @Query("DELETE FROM podcast_subscriptions")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subscriptions: List<PodcastSubscriptionEntity>)
}

@Dao
interface AppSettingsDao {

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Upsert
    suspend fun upsert(setting: AppSettingEntity)
}
