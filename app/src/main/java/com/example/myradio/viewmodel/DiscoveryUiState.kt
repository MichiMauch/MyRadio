package com.example.myradio.viewmodel

import com.example.myradio.data.model.DiscoverPodcastItem

data class DiscoveryUiState(
    val trending: List<DiscoverPodcastItem> = emptyList(),
    val recommended: List<DiscoverPodcastItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val lastUpdatedMs: Long = 0L
)
