package com.example.myradio.viewmodel

import com.example.myradio.data.model.PodcastEpisode
import com.example.myradio.data.model.PodcastSearchResult
import com.example.myradio.data.model.PodcastSubscription

data class PodcastUiState(
    val feedUrlInput: String = "",
    val isSearchPageOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<PodcastSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchErrorMessage: String? = null,
    val subscriptions: List<PodcastSubscription> = emptyList(),
    val episodes: List<PodcastEpisode> = emptyList(),
    val selectedSubscriptionId: Long? = null,
    val selectedEpisodeId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
