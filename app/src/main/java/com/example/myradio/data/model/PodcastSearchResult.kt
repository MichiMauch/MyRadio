package com.example.myradio.data.model

data class PodcastSearchResult(
    val id: Long,
    val title: String,
    val author: String,
    val feedUrl: String,
    val imageUrl: String,
    val genre: String
)
