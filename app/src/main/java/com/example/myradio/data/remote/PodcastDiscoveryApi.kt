package com.example.myradio.data.remote

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

data class TrendingPodcastResult(
    val collectionId: Long,
    val title: String,
    val author: String,
    val imageUrl: String,
    val podcastUrl: String
)

data class PodcastLookupResult(
    val collectionId: Long,
    val title: String,
    val author: String,
    val feedUrl: String,
    val imageUrl: String,
    val genre: String
)

object PodcastDiscoveryApi {

    private const val TAG = "PodcastDiscoveryApi"
    private const val TOP_BASE_URL = "https://rss.applemarketingtools.com/api/v2"
    private const val LOOKUP_BASE_URL = "https://itunes.apple.com/lookup"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun fetchTopPodcasts(country: String = "de", limit: Int = 30): List<TrendingPodcastResult> {
        val safeCountry = country.lowercase().take(2).ifBlank { "de" }
        val safeLimit = limit.coerceIn(1, 100)
        val url = "$TOP_BASE_URL/$safeCountry/podcasts/top/$safeLimit/podcasts.json"
        return try {
            val body = getBodyOrNull(url) ?: return emptyList()
            val parsed = json.decodeFromString<AppleTopPodcastsResponse>(body)
            parsed.feed.results.mapNotNull { dto ->
                val id = dto.id?.toLongOrNull() ?: return@mapNotNull null
                val title = dto.name?.trim().orEmpty()
                val author = dto.artistName?.trim().orEmpty()
                if (title.isBlank()) return@mapNotNull null
                TrendingPodcastResult(
                    collectionId = id,
                    title = title,
                    author = author,
                    imageUrl = dto.artworkUrl100?.trim().orEmpty(),
                    podcastUrl = dto.url?.trim().orEmpty()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchTopPodcasts failed", e)
            emptyList()
        }
    }

    fun lookupPodcast(collectionId: Long): PodcastLookupResult? {
        val url = "$LOOKUP_BASE_URL?id=$collectionId&entity=podcast"
        return try {
            val body = getBodyOrNull(url) ?: return null
            val parsed = json.decodeFromString<ItunesLookupResponse>(body)
            val dto = parsed.results.firstOrNull {
                (it.collectionId ?: -1L) == collectionId && !it.feedUrl.isNullOrBlank()
            } ?: parsed.results.firstOrNull { !it.feedUrl.isNullOrBlank() }
            dto ?: return null

            val feedUrl = dto.feedUrl?.trim().orEmpty()
            if (feedUrl.isBlank()) return null

            PodcastLookupResult(
                collectionId = dto.collectionId ?: collectionId,
                title = dto.collectionName?.trim().orEmpty(),
                author = dto.artistName?.trim().orEmpty(),
                feedUrl = feedUrl,
                imageUrl = dto.artworkUrl600?.trim().orEmpty(),
                genre = dto.primaryGenreName?.trim().orEmpty()
            )
        } catch (e: Exception) {
            Log.e(TAG, "lookupPodcast failed for id=$collectionId", e)
            null
        }
    }

    private fun getBodyOrNull(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
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
            if (code !in 200..299) {
                Log.e(TAG, "HTTP $code: $body")
                return null
            }
            body
        } finally {
            connection.disconnect()
        }
    }
}

@Serializable
private data class AppleTopPodcastsResponse(
    @SerialName("feed") val feed: AppleTopFeed = AppleTopFeed()
)

@Serializable
private data class AppleTopFeed(
    @SerialName("results") val results: List<AppleTopPodcastDto> = emptyList()
)

@Serializable
private data class AppleTopPodcastDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("artworkUrl100") val artworkUrl100: String? = null,
    @SerialName("url") val url: String? = null
)

@Serializable
private data class ItunesLookupResponse(
    @SerialName("results") val results: List<ItunesLookupDto> = emptyList()
)

@Serializable
private data class ItunesLookupDto(
    @SerialName("collectionId") val collectionId: Long? = null,
    @SerialName("collectionName") val collectionName: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("feedUrl") val feedUrl: String? = null,
    @SerialName("artworkUrl600") val artworkUrl600: String? = null,
    @SerialName("primaryGenreName") val primaryGenreName: String? = null
)
