package com.example.myradio.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserStationEntity::class,
        StationPreferencesEntity::class,
        DeletedDefaultEntity::class,
        AppSettingEntity::class,
        SongHistoryEntity::class,
        PodcastSubscriptionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MyRadioDatabase : RoomDatabase() {

    abstract fun stationDao(): StationDao
    abstract fun songHistoryDao(): SongHistoryDao
    abstract fun podcastSubscriptionDao(): PodcastSubscriptionDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: MyRadioDatabase? = null

        fun getInstance(context: Context): MyRadioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MyRadioDatabase::class.java,
                    "myradio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
