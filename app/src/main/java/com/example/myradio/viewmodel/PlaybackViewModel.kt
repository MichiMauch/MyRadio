package com.example.myradio.viewmodel

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.myradio.cast.CastManager
import com.example.myradio.cast.CastState
import com.example.myradio.data.local.SongHistoryStorage
import com.example.myradio.data.model.PlayedSong
import com.example.myradio.data.model.PodcastEpisode
import com.example.myradio.data.model.RadioStation
import com.example.myradio.data.repository.RadioRepository
import com.example.myradio.playback.PlaybackRoute
import com.example.myradio.playback.PlaybackService
import com.example.myradio.playback.PlaybackUiState
import com.example.myradio.playback.VisualizerManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackViewModel(
    private val repository: RadioRepository,
    private val context: Context,
    private val castManager: CastManager
) : ViewModel() {

    companion object {
        private const val TAG = "PlaybackViewModel"
    }

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _songHistory = MutableStateFlow<List<PlayedSong>>(emptyList())
    val songHistory: StateFlow<List<PlayedSong>> = _songHistory.asStateFlow()

    val waveform: StateFlow<List<Float>> = VisualizerManager.waveform

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var castJob: Job? = null
    private var castHandoverInProgress: Boolean = false

    init {
        _songHistory.value = SongHistoryStorage.loadHistory(context)
    }

    fun connectToService(context: Context) {
        if (controllerFuture != null) return
        Log.d(TAG, "connectToService: connecting...")

        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()

        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.let {
                    if (it.isDone && !it.isCancelled) it.get() else null
                }
                if (controller == null) {
                    Log.e(TAG, "connectToService: controller is null!")
                    return@addListener
                }

                mediaController = controller
                _playbackState.update { it.copy(isConnected = true) }
                Log.d(TAG, "connectToService: SUCCESS, isPlaying=${controller.isPlaying}")
                startProgressUpdates()

                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (_playbackState.value.outputRoute == PlaybackRoute.CAST) return
                        Log.d(TAG, "onIsPlayingChanged: $isPlaying")
                        _playbackState.update { it.copy(isPlaying = isPlaying) }
                        updatePlaybackTiming()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (_playbackState.value.outputRoute == PlaybackRoute.CAST) return
                        val stateStr = when (playbackState) {
                            Player.STATE_IDLE -> "IDLE"
                            Player.STATE_BUFFERING -> "BUFFERING"
                            Player.STATE_READY -> "READY"
                            Player.STATE_ENDED -> "ENDED"
                            else -> "UNKNOWN"
                        }
                        Log.d(TAG, "onPlaybackStateChanged: $stateStr")
                        _playbackState.update {
                            it.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
                        }
                        updatePlaybackTiming()
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        if (_playbackState.value.outputRoute == PlaybackRoute.CAST) return
                        val station = mediaItem?.mediaId?.toIntOrNull()?.let { id ->
                            repository.getStationById(id)
                        }
                        val metadata = mediaItem?.mediaMetadata
                        Log.d(TAG, "onMediaItemTransition: ${station?.name}")
                        _playbackState.update {
                            it.copy(
                                currentStation = station,
                                currentMediaId = mediaItem?.mediaId,
                                currentTitle = station?.name ?: metadata?.title?.toString(),
                                currentSubtitle = station?.genre
                                    ?: metadata?.subtitle?.toString()
                                    ?: metadata?.artist?.toString(),
                                currentArtworkUrl = station?.logoUrl,
                                nowPlayingTitle = null
                            )
                        }
                    }

                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        if (_playbackState.value.outputRoute == PlaybackRoute.CAST) return
                        val subtitle = mediaMetadata.subtitle?.toString()
                        val stationName = mediaMetadata.station?.toString()
                            ?: mediaMetadata.displayTitle?.toString()
                        val currentStationName = _playbackState.value.currentStation?.name
                        val currentGenre = _playbackState.value.currentStation?.genre

                        val trackTitle = subtitle?.takeIf { t ->
                            t.isNotBlank() &&
                                t != stationName &&
                                t != currentStationName &&
                                t != currentGenre
                        }

                        Log.d(TAG, "onMediaMetadataChanged: subtitle='$subtitle' station='$stationName' -> trackTitle='$trackTitle'")
                        _playbackState.update { state ->
                            state.copy(
                                currentTitle = state.currentStation?.name
                                    ?: mediaMetadata.displayTitle?.toString()
                                    ?: mediaMetadata.title?.toString(),
                                currentSubtitle = state.currentStation?.genre
                                    ?: mediaMetadata.subtitle?.toString()
                                    ?: mediaMetadata.artist?.toString(),
                                nowPlayingTitle = if (state.currentStation != null) trackTitle else null
                            )
                        }

                        if (trackTitle != null && _playbackState.value.currentStation != null) {
                            val entry = PlayedSong(
                                id = System.currentTimeMillis(),
                                stationName = _playbackState.value.currentStation?.name ?: "",
                                songTitle = trackTitle,
                                timestamp = System.currentTimeMillis(),
                                stationLogoUrl = _playbackState.value.currentStation?.logoUrl ?: ""
                            )
                            SongHistoryStorage.addEntry(this@PlaybackViewModel.context, entry)
                            _songHistory.value = SongHistoryStorage.loadHistory(this@PlaybackViewModel.context)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        if (_playbackState.value.outputRoute == PlaybackRoute.CAST) return
                        Log.e(TAG, "onPlayerError: ${error.errorCodeName} - ${error.message}", error)
                        _playbackState.update {
                            it.copy(isPlaying = false, isBuffering = false)
                        }
                    }
                })

                // Restore state if service was already playing
                if (_playbackState.value.outputRoute != PlaybackRoute.CAST && controller.currentMediaItem != null) {
                    val station = controller.currentMediaItem?.mediaId?.toIntOrNull()?.let { id ->
                        repository.getStationById(id)
                    }
                    _playbackState.update {
                        it.copy(
                            currentStation = station,
                            currentMediaId = controller.currentMediaItem?.mediaId,
                            currentTitle = station?.name,
                            currentSubtitle = station?.genre,
                            currentArtworkUrl = station?.logoUrl,
                            isPlaying = controller.isPlaying,
                            isBuffering = controller.playbackState == Player.STATE_BUFFERING
                        )
                    }
                    updatePlaybackTiming()
                }
            } catch (e: Exception) {
                Log.e(TAG, "connectToService failed", e)
            }
        }, MoreExecutors.directExecutor())
    }

    fun disconnectFromService() {
        Log.d(TAG, "disconnectFromService")
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        progressJob?.cancel()
        progressJob = null
        _playbackState.update { it.copy(isConnected = false) }
    }

    fun connectToCast() {
        castManager.start()
        if (castJob != null) return
        castJob = viewModelScope.launch {
            castManager.state.collect { castState ->
                applyCastState(castState)
            }
        }
    }

    fun disconnectFromCast() {
        castJob?.cancel()
        castJob = null
        castManager.stop()
        _playbackState.update {
            if (it.outputRoute == PlaybackRoute.CAST) {
                it.copy(
                    outputRoute = PlaybackRoute.LOCAL,
                    castDeviceName = null,
                    isCastConnected = false,
                    isPlaying = false,
                    isBuffering = false,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    nowPlayingTitle = null
                )
            } else {
                it.copy(castDeviceName = null, isCastConnected = false)
            }
        }
    }

    fun playStation(station: RadioStation) {
        if (_playbackState.value.currentStation?.id == station.id) {
            togglePlayPause()
            return
        }

        Log.d(TAG, "playStation: ${station.name} -> ${station.streamUrl}")

        if (castManager.isConnected()) {
            stopLocalPlayback()
            val castStarted = castManager.playRadio(station)
            if (castStarted) {
                repository.saveLastPlayedStationId(station.id)
                _playbackState.update {
                    it.copy(
                        outputRoute = PlaybackRoute.CAST,
                        currentStation = station,
                        currentMediaId = station.id.toString(),
                        currentTitle = station.name,
                        currentSubtitle = station.genre,
                        currentArtworkUrl = station.logoUrl,
                        nowPlayingTitle = null
                    )
                }
                return
            }
            Log.w(TAG, "playStation: cast available but start failed, fallback to local")
        }

        val controller = mediaController
        if (controller == null) {
            Log.e(TAG, "playStation: controller is null!")
            return
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(station.id.toString())
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setDisplayTitle(station.name)
                    .setArtist(station.genre)
                    .setSubtitle(station.genre)
                    .setStation(station.name)
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()

        repository.saveLastPlayedStationId(station.id)
        _playbackState.update {
            it.copy(
                outputRoute = PlaybackRoute.LOCAL,
                currentStation = station,
                currentMediaId = mediaItem.mediaId,
                currentTitle = station.name,
                currentSubtitle = station.genre,
                currentArtworkUrl = station.logoUrl,
                nowPlayingTitle = null
            )
        }
    }

    fun playPodcastEpisode(episode: PodcastEpisode, podcastImageUrl: String) {
        if (_playbackState.value.outputRoute == PlaybackRoute.CAST) {
            castManager.stopPlayback()
        }

        val controller = mediaController
        if (controller == null) {
            Log.e(TAG, "playPodcastEpisode: controller is null!")
            return
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId("podcast:${episode.id}")
            .setUri(episode.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setDisplayTitle(episode.title)
                    .setArtist(episode.podcastTitle)
                    .setSubtitle(episode.podcastTitle)
                    .setStation(episode.podcastTitle)
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()

        _playbackState.update {
            it.copy(
                outputRoute = PlaybackRoute.LOCAL,
                currentStation = null,
                currentMediaId = mediaItem.mediaId,
                currentTitle = episode.title,
                currentSubtitle = episode.podcastTitle,
                currentArtworkUrl = podcastImageUrl,
                nowPlayingTitle = null
            )
        }
    }

    fun togglePodcastEpisode(episode: PodcastEpisode, podcastImageUrl: String) {
        val episodeMediaId = "podcast:${episode.id}"
        val currentMediaId = _playbackState.value.currentMediaId
        if (currentMediaId == episodeMediaId) {
            togglePlayPause()
        } else {
            playPodcastEpisode(episode, podcastImageUrl)
        }
    }

    fun togglePlayPause() {
        if (_playbackState.value.outputRoute == PlaybackRoute.CAST) {
            if (_playbackState.value.isPlaying) {
                castManager.pause()
            } else {
                castManager.play()
            }
            return
        }

        val controller = mediaController ?: return
        Log.d(TAG, "togglePlayPause: isPlaying=${controller.isPlaying}")
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun stop() {
        Log.d(TAG, "stop")
        if (_playbackState.value.outputRoute == PlaybackRoute.CAST) {
            castManager.stopPlayback()
        } else {
            val controller = mediaController ?: return
            controller.stop()
            controller.clearMediaItems()
        }
        _playbackState.update {
            it.copy(
                currentStation = null,
                currentMediaId = null,
                currentTitle = null,
                currentSubtitle = null,
                currentArtworkUrl = null,
                currentPositionMs = 0L,
                durationMs = 0L,
                isPlaying = false,
                isBuffering = false,
                nowPlayingTitle = null
            )
        }
    }

    fun clearHistory() {
        SongHistoryStorage.clearHistory(context)
        _songHistory.value = emptyList()
    }

    private fun applyCastState(castState: CastState) {
        _playbackState.update { current ->
            var updated = current.copy(
                isCastConnected = castState.isConnected,
                castDeviceName = castState.deviceName
            )

            if (!castState.isConnected && current.outputRoute == PlaybackRoute.CAST) {
                castHandoverInProgress = false
                updated = updated.copy(
                    outputRoute = PlaybackRoute.LOCAL,
                    isPlaying = false,
                    isBuffering = false,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    nowPlayingTitle = null
                )
                return@update updated
            }

            if (!castState.isConnected) {
                castHandoverInProgress = false
            }

            if (castState.isConnected && !castState.hasActiveMedia) {
                val canHandover =
                    !castHandoverInProgress &&
                        current.outputRoute == PlaybackRoute.LOCAL &&
                        current.currentStation != null &&
                        (current.isPlaying || current.isBuffering || !current.currentMediaId.isNullOrBlank())

                if (canHandover) {
                    val started = castManager.playRadio(current.currentStation)
                    if (started) {
                        castHandoverInProgress = true
                        stopLocalPlayback()
                    }
                }
            }

            if (castState.isConnected && castState.hasActiveMedia) {
                castHandoverInProgress = false
                stopLocalPlayback()
                val station = castState.currentStationId?.let { repository.getStationById(it) }
                updated = updated.copy(
                    outputRoute = PlaybackRoute.CAST,
                    currentStation = station,
                    currentMediaId = station?.id?.toString() ?: updated.currentMediaId,
                    currentTitle = station?.name ?: castState.currentTitle ?: updated.currentTitle,
                    currentSubtitle = station?.genre ?: castState.currentSubtitle ?: updated.currentSubtitle,
                    currentArtworkUrl = station?.logoUrl
                        ?: castState.currentArtworkUrl
                        ?: updated.currentArtworkUrl,
                    nowPlayingTitle = null,
                    isPlaying = castState.isPlaying,
                    isBuffering = castState.isBuffering,
                    currentPositionMs = castState.currentPositionMs,
                    durationMs = castState.durationMs
                )
            }

            updated
        }
    }

    private fun stopLocalPlayback() {
        val controller = mediaController ?: return
        runCatching {
            controller.pause()
            controller.stop()
            controller.clearMediaItems()
        }.onFailure {
            Log.w(TAG, "stopLocalPlayback: failed to stop local player cleanly", it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        disconnectFromService()
        disconnectFromCast()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                updatePlaybackTiming()
                delay(1000)
            }
        }
    }

    private fun updatePlaybackTiming() {
        if (_playbackState.value.outputRoute == PlaybackRoute.CAST) return
        val controller = mediaController ?: return
        val rawDuration = controller.duration
        val duration = if (rawDuration > 0L) rawDuration else 0L
        val position = controller.currentPosition.coerceAtLeast(0L)
        _playbackState.update {
            it.copy(
                currentPositionMs = position,
                durationMs = duration
            )
        }
    }
}
