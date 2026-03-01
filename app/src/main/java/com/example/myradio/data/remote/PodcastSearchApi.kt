package com.example.myradio.data.remote

import android.util.Log
import com.example.myradio.data.model.PodcastSearchResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object PodcastSearchApi {

    private const val TAG = "PodcastSearchApi"
    private const val BASE_URL = "https://itunes.apple.com/search"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun searchPodcasts(query: String, limit: Int = 30): List<PodcastSearchResult> {
        if (query.isBlank()) return emptyList()

        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url =
            "$BASE_URL?media=podcast&entity=podcast&term=$encoded&limit=$limit"

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
            val response = json.decodeFromString<ItunesSearchResponse>(body)
            response.results.mapNotNull { dto ->
                val title = dto.collectionName?.trim().orEmpty()
                val feedUrl = dto.feedUrl?.trim().orEmpty()
                if (title.isBlank() || feedUrl.isBlank()) return@mapNotNull null
                PodcastSearchResult(
                    id = dto.collectionId ?: title.hashCode().toLong(),
                    title = title,
                    author = dto.artistName?.trim().orEmpty(),
                    feedUrl = feedUrl,
                    imageUrl = dto.artworkUrl600?.trim().orEmpty(),
                    genre = dto.primaryGenreName?.trim().orEmpty()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchPodcasts failed", e)
            emptyList()
        }
    }
}

@Serializable
private data class ItunesSearchResponse(
    @SerialName("resultCount") val resultCount: Int = 0,
    @SerialName("results") val results: List<ItunesPodcastDto> = emptyList()
)

@Serializable
private data class ItunesPodcastDto(
    @SerialName("collectionId") val collectionId: Long? = null,
    @SerialName("collectionName") val collectionName: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("feedUrl") val feedUrl: String? = null,
    @SerialName("artworkUrl600") val artworkUrl600: String? = null,
    @SerialName("primaryGenreName") val primaryGenreName: String? = null
)
