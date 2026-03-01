package com.example.myradio.data.repository

import android.content.Context
import com.example.myradio.data.local.PodcastSubscriptionsStorage
import com.example.myradio.data.model.DiscoverPodcastItem
import com.example.myradio.data.model.DiscoverSource
import com.example.myradio.data.model.PodcastEpisode
import com.example.myradio.data.model.PodcastSearchResult
import com.example.myradio.data.model.PodcastSubscription
import com.example.myradio.data.remote.PodcastDiscoveryApi
import com.example.myradio.data.remote.PodcastFetchResult
import com.example.myradio.data.remote.PodcastFeedResult
import com.example.myradio.data.remote.PodcastRssApi
import com.example.myradio.data.remote.PodcastSearchApi

class PodcastRepository(private val context: Context) {

    fun loadSubscriptions(): List<PodcastSubscription> =
        PodcastSubscriptionsStorage.load(context)

    fun saveSubscriptions(subscriptions: List<PodcastSubscription>) {
        PodcastSubscriptionsStorage.save(context, subscriptions)
    }

    fun fetchFeed(feedUrl: String): PodcastFeedResult? =
        PodcastRssApi.fetchFeed(feedUrl)

    fun fetchFeedDetailed(feedUrl: String): PodcastFetchResult =
        PodcastRssApi.fetchFeedDetailed(feedUrl)

    fun searchPodcasts(query: String): List<PodcastSearchResult> =
        PodcastSearchApi.searchPodcasts(query)

    fun fetchTrendingPodcasts(country: String = "de", limit: Int = 30): List<DiscoverPodcastItem> {
        val trending = PodcastDiscoveryApi.fetchTopPodcasts(country = country, limit = limit)
        return trending.map { item ->
            val lookup = PodcastDiscoveryApi.lookupPodcast(item.collectionId)
            DiscoverPodcastItem(
                id = "trending:${item.collectionId}",
                collectionId = item.collectionId,
                title = lookup?.title?.ifBlank { item.title } ?: item.title,
                author = lookup?.author?.ifBlank { item.author } ?: item.author,
                imageUrl = lookup?.imageUrl?.ifBlank { item.imageUrl } ?: item.imageUrl,
                feedUrl = lookup?.feedUrl,
                genre = lookup?.genre.orEmpty(),
                source = DiscoverSource.TRENDING
            )
        }
    }

    fun fetchRecommendations(
        subscriptions: List<PodcastSubscription>,
        trending: List<DiscoverPodcastItem>,
        limit: Int = 20
    ): List<DiscoverPodcastItem> {
        if (subscriptions.isEmpty()) return emptyList()

        val genreCounts = linkedMapOf<String, Int>()
        val authorCounts = linkedMapOf<String, Int>()

        subscriptions.take(8).forEach { sub ->
            val candidates = PodcastSearchApi.searchPodcasts(sub.title, limit = 8)
            val matched = candidates.firstOrNull {
                it.feedUrl.normalizeFeedUrl() == sub.feedUrl.normalizeFeedUrl()
            } ?: candidates.firstOrNull()
            if (matched != null) {
                matched.genre.takeIf { it.isNotBlank() }?.let { genre ->
                    genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
                }
                matched.author.takeIf { it.isNotBlank() }?.let { author ->
                    authorCounts[author] = (authorCounts[author] ?: 0) + 1
                }
            }
        }

        val topGenres = genreCounts.entries.sortedByDescending { it.value }.map { it.key }.take(3)
        val topAuthors = authorCounts.entries.sortedByDescending { it.value }.map { it.key }.take(3)

        val subscribed = subscriptions.map { it.feedUrl.normalizeFeedUrl() }.toSet()

        val candidates = mutableListOf<DiscoverPodcastItem>()
        candidates += trending.filter { !it.feedUrl.isNullOrBlank() }

        val queries = (topGenres.take(2) + topAuthors.take(2)).distinct()
        queries.forEach { query ->
            PodcastSearchApi.searchPodcasts(query, limit = 20).forEach { result ->
                candidates += DiscoverPodcastItem(
                    id = "rec:${result.id}:${result.feedUrl.normalizeFeedUrl()}",
                    collectionId = result.id,
                    title = result.title,
                    author = result.author,
                    imageUrl = result.imageUrl,
                    feedUrl = result.feedUrl,
                    genre = result.genre,
                    source = DiscoverSource.RECOMMENDED
                )
            }
        }

        return candidates
            .filter { !it.feedUrl.isNullOrBlank() }
            .filterNot { subscribed.contains(it.feedUrl.orEmpty().normalizeFeedUrl()) }
            .distinctBy { it.feedUrl.orEmpty().normalizeFeedUrl() }
            .mapNotNull { candidate ->
                val score = recommendationScore(candidate, topGenres, topAuthors)
                if (score <= 0) return@mapNotNull null
                val reason = recommendationReason(candidate, topGenres, topAuthors)
                candidate.copy(
                    source = DiscoverSource.RECOMMENDED,
                    reason = reason
                ) to score
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(limit)
    }

    fun fetchEpisodesForSubscriptions(subscriptions: List<PodcastSubscription>): List<PodcastEpisode> {
        return subscriptions.flatMap { subscription ->
            PodcastRssApi.fetchFeed(subscription.feedUrl)?.episodes.orEmpty()
        }.sortedByDescending { it.publishedAt }
    }

    private fun recommendationScore(
        item: DiscoverPodcastItem,
        topGenres: List<String>,
        topAuthors: List<String>
    ): Int {
        var score = 0
        if (item.genre.isNotBlank() && topGenres.any { it.equals(item.genre, ignoreCase = true) }) score += 3
        if (item.author.isNotBlank() && topAuthors.any { it.equals(item.author, ignoreCase = true) }) score += 4
        if (item.source == DiscoverSource.TRENDING) score += 2
        return score
    }

    private fun recommendationReason(
        item: DiscoverPodcastItem,
        topGenres: List<String>,
        topAuthors: List<String>
    ): String {
        val genreHit = item.genre.isNotBlank() && topGenres.any { it.equals(item.genre, ignoreCase = true) }
        val authorHit = item.author.isNotBlank() && topAuthors.any { it.equals(item.author, ignoreCase = true) }
        return when {
            authorHit && genreHit -> "Gleicher Publisher + ähnliches Genre"
            authorHit -> "Gleicher Publisher"
            genreHit -> "Ähnliches Genre"
            else -> "Für dich empfohlen"
        }
    }
}

private fun String.normalizeFeedUrl(): String =
    trim().trimEnd('/').lowercase()
