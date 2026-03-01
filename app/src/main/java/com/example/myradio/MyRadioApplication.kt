package com.example.myradio

import android.app.Application
import android.util.Log
import com.example.myradio.cast.CastManager
import com.example.myradio.data.local.db.JsonToRoomMigration
import com.example.myradio.data.local.db.MyRadioDatabase
import com.example.myradio.data.repository.PodcastRepository
import com.example.myradio.data.repository.RadioRepository
import com.example.myradio.playback.NetworkConnectivityMonitor
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyRadioApplication : Application() {
    companion object {
        private const val TAG = "MyRadioApplication"
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { MyRadioDatabase.getInstance(this) }
    val radioRepository by lazy { RadioRepository(this, database, applicationScope) }
    val podcastRepository by lazy { PodcastRepository(this, database) }
    val castManager by lazy { CastManager(this) }
    val networkMonitor by lazy { NetworkConnectivityMonitor(this) }

    override fun onCreate() {
        super.onCreate()
        networkMonitor.start()

        applicationScope.launch(Dispatchers.IO) {
            JsonToRoomMigration.migrateIfNeeded(this@MyRadioApplication, database)
        }

        runCatching {
            CastContext.getSharedInstance(this)
        }.onFailure {
            Log.w(TAG, "Cast initialization unavailable, app continues without Cast.", it)
        }
    }
}
