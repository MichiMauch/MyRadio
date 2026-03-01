package com.example.myradio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayedSong(
    val id: Long,
    val stationName: String,
    val songTitle: String,
    val timestamp: Long,
    val stationLogoUrl: String = ""
)
