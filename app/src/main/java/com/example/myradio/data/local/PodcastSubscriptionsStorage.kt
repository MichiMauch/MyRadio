package com.example.myradio.data.local

import android.content.Context
import android.util.Log
import com.example.myradio.data.model.PodcastSubscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object PodcastSubscriptionsStorage {

    private const val TAG = "PodcastSubscriptions"
    private const val FILE_NAME = "podcast_subscriptions.json"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun load(context: Context): List<PodcastSubscription> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load subscriptions", e)
            emptyList()
        }
    }

    fun save(context: Context, subscriptions: List<PodcastSubscription>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(json.encodeToString(subscriptions))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save subscriptions", e)
        }
    }
}
