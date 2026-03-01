package com.example.myradio.data.model

data class PodcastEpisode(
    val id: String,
    val feedUrl: String,
    val podcastTitle: String,
    val title: String,
    val audioUrl: String,
    val description: String,
    val publishedAt: Long,
    val imageUrl: String = "",
    val durationSec: Long? = null
)
