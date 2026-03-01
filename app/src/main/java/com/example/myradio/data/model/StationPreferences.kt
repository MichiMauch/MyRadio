package com.example.myradio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StationPreferences(
    val isFavorite: Boolean = false,
    val sortOrder: Int = Int.MAX_VALUE
)
