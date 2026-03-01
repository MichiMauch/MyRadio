package com.example.myradio.data.local.db

import com.example.myradio.data.model.PlayedSong
import com.example.myradio.data.model.PodcastSubscription
import com.example.myradio.data.model.RadioStation
import com.example.myradio.data.model.StationPreferences

fun UserStationEntity.toRadioStation(): RadioStation = RadioStation(
    id = id,
    name = name,
    streamUrl = streamUrl,
    genre = genre,
    logoUrl = logoUrl,
    country = country
)

fun RadioStation.toEntity(): UserStationEntity = UserStationEntity(
    id = id,
    name = name,
    streamUrl = streamUrl,
    genre = genre,
    logoUrl = logoUrl,
    country = country
)

fun StationPreferencesEntity.toModel(): StationPreferences = StationPreferences(
    isFavorite = isFavorite,
    sortOrder = sortOrder
)

fun SongHistoryEntity.toPlayedSong(): PlayedSong = PlayedSong(
    id = id,
    stationName = stationName,
    songTitle = songTitle,
    timestamp = timestamp,
    stationLogoUrl = stationLogoUrl
)

fun PlayedSong.toEntity(): SongHistoryEntity = SongHistoryEntity(
    id = 0, // autoGenerate
    stationName = stationName,
    songTitle = songTitle,
    timestamp = timestamp,
    stationLogoUrl = stationLogoUrl
)

fun PodcastSubscriptionEntity.toModel(): PodcastSubscription = PodcastSubscription(
    id = id,
    feedUrl = feedUrl,
    title = title,
    description = description,
    imageUrl = imageUrl
)

fun PodcastSubscription.toEntity(): PodcastSubscriptionEntity = PodcastSubscriptionEntity(
    id = id,
    feedUrl = feedUrl,
    title = title,
    description = description,
    imageUrl = imageUrl
)
