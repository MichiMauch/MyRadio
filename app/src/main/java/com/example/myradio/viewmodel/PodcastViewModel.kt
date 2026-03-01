package com.example.myradio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myradio.data.model.DiscoverPodcastItem
import com.example.myradio.data.model.PodcastSearchResult
import com.example.myradio.data.model.PodcastSubscription
import com.example.myradio.data.repository.PodcastRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PodcastViewModel(
    private val podcastRepository: PodcastRepository
) : ViewModel() {

    companion object {
        private const val DISCOVERY_TTL_MS = 6 * 60 * 60 * 1000L
    }

    private val _podcastState = MutableStateFlow(PodcastUiState())
    val podcastState: StateFlow<PodcastUiState> = _podcastState.asStateFlow()

    private val _discoveryState = MutableStateFlow(DiscoveryUiState())
    val discoveryState: StateFlow<DiscoveryUiState> = _discoveryState.asStateFlow()

    init {
        viewModelScope.launch {
            val subscriptions = withContext(Dispatchers.IO) {
                podcastRepository.loadSubscriptions()
            }
            _podcastState.update { it.copy(subscriptions = subscriptions) }
            refreshPodcastEpisodes()
        }
    }

    // --- Podcast Feed ---

    fun updatePodcastFeedUrl(value: String) {
        _podcastState.update { it.copy(feedUrlInput = value, errorMessage = null) }
    }

    fun openPodcastSearch() {
        _podcastState.update {
            it.copy(
                isSearchPageOpen = true,
                searchErrorMessage = null,
                errorMessage = null
            )
        }
    }

    fun closePodcastSearch() {
        _podcastState.update {
            it.copy(
                isSearchPageOpen = false,
                isSearching = false,
                searchErrorMessage = null
            )
        }
    }

    fun updatePodcastSearchQuery(value: String) {
        _podcastState.update { it.copy(searchQuery = value, searchErrorMessage = null) }
    }

    fun searchPodcasts() {
        val query = _podcastState.value.searchQuery.trim()
        if (query.isBlank()) {
            _podcastState.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearching = false,
                    searchErrorMessage = "Bitte Suchbegriff eingeben"
                )
            }
            return
        }

        _podcastState.update {
            it.copy(
                isSearching = true,
                searchErrorMessage = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                podcastRepository.searchPodcasts(query)
            }
            _podcastState.update {
                it.copy(
                    searchResults = results,
                    isSearching = false,
                    searchErrorMessage = if (results.isEmpty()) "Keine Podcasts gefunden" else null
                )
            }
        }
    }

    fun selectPodcastSubscription(subscriptionId: Long?) {
        _podcastState.update {
            it.copy(
                selectedSubscriptionId = subscriptionId,
                selectedEpisodeId = null
            )
        }
    }

    fun selectPodcastEpisode(episodeId: String?) {
        _podcastState.update { it.copy(selectedEpisodeId = episodeId) }
    }

    fun addPodcastSubscription() {
        val isSearchPage = _podcastState.value.isSearchPageOpen
        subscribePodcastByFeedUrl(
            feedUrlRaw = _podcastState.value.feedUrlInput,
            selectNewSubscription = !isSearchPage,
            closeSearchAfterSuccess = isSearchPage
        )
    }

    fun subscribeFromSearch(result: PodcastSearchResult) {
        subscribePodcastByFeedUrl(
            feedUrlRaw = result.feedUrl,
            selectNewSubscription = false,
            closeSearchAfterSuccess = true
        )
    }

    fun refreshPodcastEpisodes() {
        val subscriptions = _podcastState.value.subscriptions
        if (subscriptions.isEmpty()) {
            _podcastState.update {
                it.copy(
                    episodes = emptyList(),
                    selectedSubscriptionId = null,
                    selectedEpisodeId = null,
                    isLoading = false
                )
            }
            return
        }

        _podcastState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val episodes = withContext(Dispatchers.IO) {
                podcastRepository.fetchEpisodesForSubscriptions(subscriptions)
            }
            _podcastState.update {
                val keepEpisodeId = it.selectedEpisodeId?.takeIf { selectedId ->
                    episodes.any { ep -> ep.id == selectedId }
                }
                it.copy(
                    episodes = episodes,
                    selectedEpisodeId = keepEpisodeId,
                    isLoading = false,
                    errorMessage = if (episodes.isEmpty()) "Keine Episoden gefunden" else null
                )
            }
        }
    }

    fun removePodcastSubscription(id: Long) {
        val updated = _podcastState.value.subscriptions.filterNot { it.id == id }
        viewModelScope.launch(Dispatchers.IO) {
            podcastRepository.saveSubscriptions(updated)
        }
        _podcastState.update {
            it.copy(
                subscriptions = updated,
                selectedSubscriptionId = if (it.selectedSubscriptionId == id) null else it.selectedSubscriptionId,
                selectedEpisodeId = if (it.selectedSubscriptionId == id) null else it.selectedEpisodeId
            )
        }
        val subscribed = updated.map { it.feedUrl.normalizeFeedUrl() }.toSet()
        _discoveryState.update {
            it.copy(
                trending = markSubscribed(it.trending, subscribed),
                recommended = markSubscribed(it.recommended, subscribed)
            )
        }
        refreshPodcastEpisodes()
    }

    // --- Discovery ---

    fun loadDiscovery(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val current = _discoveryState.value
        val cacheValid = current.lastUpdatedMs > 0L &&
            (now - current.lastUpdatedMs) < DISCOVERY_TTL_MS &&
            (current.trending.isNotEmpty() || current.recommended.isNotEmpty())
        if (!force && cacheValid) return

        _discoveryState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            val subscriptions = _podcastState.value.subscriptions
            val (trending, recommended) = withContext(Dispatchers.IO) {
                val trendingItems = podcastRepository.fetchTrendingPodcasts(country = "de", limit = 30)
                val recommendationItems = podcastRepository.fetchRecommendations(
                    subscriptions = subscriptions,
                    trending = trendingItems,
                    limit = 20
                )
                trendingItems to recommendationItems
            }

            val subscribedFeeds = subscriptions.map { it.feedUrl.normalizeFeedUrl() }.toSet()
            _discoveryState.update {
                it.copy(
                    trending = markSubscribed(trending, subscribedFeeds),
                    recommended = markSubscribed(recommended, subscribedFeeds),
                    isLoading = false,
                    errorMessage = if (trending.isEmpty() && recommended.isEmpty()) {
                        "Keine Vorschläge geladen"
                    } else {
                        null
                    },
                    lastUpdatedMs = now
                )
            }
        }
    }

    fun refreshDiscovery() {
        loadDiscovery(force = true)
    }

    fun clearDiscoveryInfoMessage() {
        _discoveryState.update { it.copy(infoMessage = null) }
    }

    fun subscribeFromDiscovery(itemId: String) {
        val item = (_discoveryState.value.trending + _discoveryState.value.recommended)
            .firstOrNull { it.id == itemId }
        if (item == null) {
            _discoveryState.update { it.copy(errorMessage = "Podcast nicht gefunden") }
            return
        }
        if (item.isSubscribed) {
            _discoveryState.update { it.copy(infoMessage = "Bereits abonniert") }
            return
        }

        val feedUrl = item.feedUrl.orEmpty()
        if (feedUrl.isBlank()) {
            _discoveryState.update {
                it.copy(errorMessage = "Feed konnte nicht aufgelöst werden")
            }
            return
        }

        subscribePodcastByFeedUrl(
            feedUrlRaw = feedUrl,
            selectNewSubscription = false,
            closeSearchAfterSuccess = false,
            onSuccess = {
                _discoveryState.update { state ->
                    val subscribed = _podcastState.value.subscriptions
                        .map { it.feedUrl.normalizeFeedUrl() }
                        .toSet()
                    state.copy(
                        trending = markSubscribed(state.trending, subscribed),
                        recommended = markSubscribed(state.recommended, subscribed),
                        infoMessage = "\"${item.title}\" abonniert",
                        errorMessage = null
                    )
                }
            },
            onFailure = { message ->
                _discoveryState.update { it.copy(errorMessage = message) }
            }
        )
    }

    // --- Private helpers ---

    private fun subscribePodcastByFeedUrl(
        feedUrlRaw: String,
        selectNewSubscription: Boolean,
        closeSearchAfterSuccess: Boolean,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {
        val feedUrl = feedUrlRaw.trim()
        if (feedUrl.isBlank()) {
            val message = "Bitte RSS-URL eingeben"
            _podcastState.update { it.copy(errorMessage = message) }
            onFailure?.invoke(message)
            return
        }
        val normalizedFeed = feedUrl.normalizeFeedUrl()
        if (_podcastState.value.subscriptions.any { it.feedUrl.normalizeFeedUrl() == normalizedFeed }) {
            val message = "Feed bereits abonniert"
            _podcastState.update { it.copy(errorMessage = message) }
            onFailure?.invoke(message)
            return
        }

        _podcastState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                podcastRepository.fetchFeedDetailed(feedUrl)
            }
            if (result.feed == null) {
                val message = "Feed konnte nicht geladen werden: ${result.errorMessage ?: "Unbekannter Fehler"}"
                _podcastState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                }
                onFailure?.invoke(message)
                return@launch
            }

            val current = _podcastState.value.subscriptions
            val newSubscription = PodcastSubscription(
                id = System.currentTimeMillis(),
                feedUrl = feedUrl,
                title = result.feed.title,
                description = result.feed.description,
                imageUrl = result.feed.imageUrl
            )
            val updatedSubscriptions = (listOf(newSubscription) + current)
                .distinctBy { it.feedUrl.normalizeFeedUrl() }

            withContext(Dispatchers.IO) {
                podcastRepository.saveSubscriptions(updatedSubscriptions)
            }

            _podcastState.update {
                it.copy(
                    feedUrlInput = "",
                    subscriptions = updatedSubscriptions,
                    episodes = result.feed.episodes + it.episodes,
                    selectedSubscriptionId = if (selectNewSubscription) newSubscription.id else null,
                    selectedEpisodeId = null,
                    isLoading = false,
                    errorMessage = null,
                    isSearchPageOpen = if (closeSearchAfterSuccess) false else it.isSearchPageOpen,
                    searchQuery = if (closeSearchAfterSuccess) "" else it.searchQuery,
                    searchResults = if (closeSearchAfterSuccess) emptyList() else it.searchResults,
                    searchErrorMessage = null
                )
            }
            val subscribed = updatedSubscriptions.map { it.feedUrl.normalizeFeedUrl() }.toSet()
            _discoveryState.update {
                it.copy(
                    trending = markSubscribed(it.trending, subscribed),
                    recommended = markSubscribed(it.recommended, subscribed)
                )
            }
            onSuccess?.invoke()
        }
    }

    private fun markSubscribed(
        items: List<DiscoverPodcastItem>,
        subscribedFeeds: Set<String>
    ): List<DiscoverPodcastItem> {
        return items.map { item ->
            val normalized = item.feedUrl.orEmpty().normalizeFeedUrl()
            item.copy(isSubscribed = normalized.isNotBlank() && subscribedFeeds.contains(normalized))
        }
    }
}

private fun String.normalizeFeedUrl(): String =
    trim().trimEnd('/').lowercase()
