package com.example.myradio

import android.app.Application
import android.util.Log
import com.example.myradio.cast.CastManager
import com.example.myradio.data.repository.PodcastRepository
import com.example.myradio.data.repository.RadioRepository
import com.google.android.gms.cast.framework.CastContext

class MyRadioApplication : Application() {
    companion object {
        private const val TAG = "MyRadioApplication"
    }

    val radioRepository by lazy { RadioRepository(this) }
    val podcastRepository by lazy { PodcastRepository(this) }
    val castManager by lazy { CastManager(this) }

    override fun onCreate() {
        super.onCreate()
        runCatching {
            CastContext.getSharedInstance(this)
        }.onFailure {
            Log.w(TAG, "Cast initialization unavailable, app continues without Cast.", it)
        }
    }
}
