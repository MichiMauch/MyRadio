package com.example.myradio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PodcastSubscription(
    val id: Long,
    val feedUrl: String,
    val title: String,
    val description: String = "",
    val imageUrl: String = ""
)
