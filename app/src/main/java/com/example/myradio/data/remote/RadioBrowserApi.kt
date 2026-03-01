package com.example.myradio.data.remote

import android.util.Log
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object RadioBrowserApi {

    private const val TAG = "RadioBrowserApi"
    private const val BASE_URL = "https://de1.api.radio-browser.info"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun searchStations(nameQuery: String, limit: Int = 30): List<RadioBrowserStationDto> {
        if (nameQuery.isBlank()) return emptyList()

        val encoded = URLEncoder.encode(nameQuery.trim(), "UTF-8")
        val url =
            "$BASE_URL/json/stations/search?name=$encoded&limit=$limit&hidebroken=true&order=clickcount&reverse=true"

        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")

            val code = connection.responseCode
            val body = if (code in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText()
            }
            connection.disconnect()

            if (code !in 200..299) {
                Log.e(TAG, "HTTP $code: $body")
                return emptyList()
            }

            if (body.isNullOrBlank()) return emptyList()
            json.decodeFromString<List<RadioBrowserStationDto>>(body)
        } catch (e: Exception) {
            Log.e(TAG, "searchStations failed", e)
            emptyList()
        }
    }
}
