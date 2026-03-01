package com.example.myradio.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RadioBrowserStationDto(
    val name: String = "",
    @SerialName("url_resolved") val urlResolved: String = "",
    val tags: String = "",
    @SerialName("countrycode") val countryCode: String = "",
    val favicon: String = ""
)
