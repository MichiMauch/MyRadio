package com.example.myradio.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class UserStationEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val streamUrl: String,
    val genre: String,
    val logoUrl: String,
    val country: String
)

@Entity(tableName = "station_preferences")
data class StationPreferencesEntity(
    @PrimaryKey val stationId: Int,
    val isFavorite: Boolean,
    val sortOrder: Int
)

@Entity(tableName = "deleted_defaults")
data class DeletedDefaultEntity(
    @PrimaryKey val stationId: Int
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(
    tableName = "song_history",
    indices = [Index(value = ["timestamp"])]
)
data class SongHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stationName: String,
    val songTitle: String,
    val timestamp: Long,
    val stationLogoUrl: String
)

@Entity(
    tableName = "podcast_subscriptions",
    indices = [Index(value = ["feedUrl"], unique = true)]
)
data class PodcastSubscriptionEntity(
    @PrimaryKey val id: Long,
    val feedUrl: String,
    val title: String,
    val description: String,
    val imageUrl: String
)
