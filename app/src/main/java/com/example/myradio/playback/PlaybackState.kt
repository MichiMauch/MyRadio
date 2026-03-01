package com.example.myradio.playback

import com.example.myradio.data.model.RadioStation

data class PlaybackUiState(
    val currentStation: RadioStation? = null,
    val currentMediaId: String? = null,
    val currentTitle: String? = null,
    val currentSubtitle: String? = null,
    val currentArtworkUrl: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isConnected: Boolean = false,
    val nowPlayingTitle: String? = null,
    val outputRoute: PlaybackRoute = PlaybackRoute.LOCAL,
    val castDeviceName: String? = null,
    val isCastConnected: Boolean = false
)
