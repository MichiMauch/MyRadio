package com.example.myradio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RadioStation(
    val id: Int,
    val name: String,
    val streamUrl: String,
    val genre: String,
    val logoUrl: String = "",
    val country: String = "",
    val isFavorite: Boolean = false,
    val sortOrder: Int = Int.MAX_VALUE
)
