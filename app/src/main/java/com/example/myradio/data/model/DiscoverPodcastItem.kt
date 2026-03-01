package com.example.myradio.data.model

data class DiscoverPodcastItem(
    val id: String,
    val collectionId: Long?,
    val title: String,
    val author: String,
    val imageUrl: String,
    val feedUrl: String?,
    val genre: String,
    val source: DiscoverSource,
    val reason: String? = null,
    val isSubscribed: Boolean = false
)

enum class DiscoverSource {
    TRENDING,
    RECOMMENDED
}
