package com.example.myradio.cast

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.myradio.data.model.RadioStation
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CastState(
    val isConnected: Boolean = false,
    val deviceName: String? = null,
    val hasActiveMedia: Boolean = false,
    val currentStationId: Int? = null,
    val currentTitle: String? = null,
    val currentSubtitle: String? = null,
    val currentArtworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L
)

class CastManager(
    context: Context
) {
    companion object {
        private const val TAG = "CastManager"
    }

    private val appContext = context.applicationContext
    private val castContext: CastContext? = runCatching {
        CastContext.getSharedInstance(appContext)
    }.onFailure {
        Log.w(TAG, "CastContext unavailable, Cast features disabled.", it)
    }.getOrNull()

    private val sessionManager get() = castContext?.sessionManager

    private val _state = MutableStateFlow(CastState())
    val state: StateFlow<CastState> = _state.asStateFlow()

    private var started = false
    private var remoteMediaClient: RemoteMediaClient? = null
    private var lastStationId: Int? = null

    private val remoteCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() = syncRemoteState()
        override fun onMetadataUpdated() = syncRemoteState()
    }

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            attachToSession(session)
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            attachToSession(session)
        }

        override fun onSessionEnding(session: CastSession) = Unit

        override fun onSessionEnded(session: CastSession, error: Int) {
            detachRemoteMediaClient()
            lastStationId = null
            _state.value = CastState()
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            detachRemoteMediaClient()
            _state.value = CastState()
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            detachRemoteMediaClient()
            _state.value = CastState()
        }

        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
    }

    fun start() {
        if (started) return
        val manager = sessionManager ?: return
        started = true
        manager.addSessionManagerListener(sessionListener, CastSession::class.java)
        attachToSession(manager.currentCastSession)
    }

    fun stop() {
        if (!started) return
        sessionManager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        detachRemoteMediaClient()
        started = false
    }

    fun isConnected(): Boolean = _state.value.isConnected

    fun playRadio(station: RadioStation): Boolean {
        val client = remoteMediaClient ?: return false

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, station.name)
            putString(MediaMetadata.KEY_SUBTITLE, station.genre)
            if (station.logoUrl.isNotBlank()) {
                addImage(WebImage(Uri.parse(station.logoUrl)))
            }
        }

        val mediaInfo = MediaInfo.Builder(station.streamUrl)
            .setContentType("audio/*")
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setMetadata(metadata)
            .build()

        client.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build()
        )

        lastStationId = station.id
        _state.update {
            it.copy(
                isConnected = true,
                deviceName = sessionManager?.currentCastSession?.castDevice?.friendlyName,
                hasActiveMedia = true,
                currentStationId = station.id,
                currentTitle = station.name,
                currentSubtitle = station.genre,
                currentArtworkUrl = station.logoUrl.ifBlank { null }
            )
        }
        syncRemoteState()
        return true
    }

    fun play(): Boolean {
        val client = remoteMediaClient ?: return false
        client.play()
        syncRemoteState()
        return true
    }

    fun pause(): Boolean {
        val client = remoteMediaClient ?: return false
        client.pause()
        syncRemoteState()
        return true
    }

    fun stopPlayback(): Boolean {
        val client = remoteMediaClient ?: return false
        client.stop()
        _state.update {
            it.copy(
                hasActiveMedia = false,
                isPlaying = false,
                isBuffering = false,
                currentPositionMs = 0L,
                durationMs = 0L
            )
        }
        return true
    }

    private fun attachToSession(session: CastSession?) {
        detachRemoteMediaClient()
        if (session == null || !session.isConnected) {
            _state.value = CastState()
            return
        }

        remoteMediaClient = session.remoteMediaClient
        remoteMediaClient?.registerCallback(remoteCallback)

        _state.update {
            it.copy(
                isConnected = true,
                deviceName = session.castDevice?.friendlyName
            )
        }
        syncRemoteState()
    }

    private fun detachRemoteMediaClient() {
        remoteMediaClient?.unregisterCallback(remoteCallback)
        remoteMediaClient = null
    }

    private fun syncRemoteState() {
        val manager = sessionManager
        if (manager == null) {
            _state.value = CastState()
            return
        }
        val session = manager.currentCastSession
        val client = remoteMediaClient

        if (session == null || !session.isConnected || client == null) {
            _state.value = CastState()
            return
        }

        val mediaStatus = client.mediaStatus
        val metadata = client.mediaInfo?.metadata
        val title = metadata?.getString(MediaMetadata.KEY_TITLE)
        val subtitle = metadata?.getString(MediaMetadata.KEY_SUBTITLE)
        val artwork = metadata?.images?.firstOrNull()?.url?.toString()
        val playerState = mediaStatus?.playerState ?: MediaStatus.PLAYER_STATE_IDLE
        val duration = (client.mediaInfo?.streamDuration ?: 0L).coerceAtLeast(0L)

        _state.update {
            it.copy(
                isConnected = true,
                deviceName = session.castDevice?.friendlyName,
                hasActiveMedia = mediaStatus != null && playerState != MediaStatus.PLAYER_STATE_IDLE,
                currentStationId = it.currentStationId ?: lastStationId,
                currentTitle = title ?: it.currentTitle,
                currentSubtitle = subtitle ?: it.currentSubtitle,
                currentArtworkUrl = artwork ?: it.currentArtworkUrl,
                isPlaying = playerState == MediaStatus.PLAYER_STATE_PLAYING,
                isBuffering = playerState == MediaStatus.PLAYER_STATE_BUFFERING ||
                    playerState == MediaStatus.PLAYER_STATE_LOADING,
                currentPositionMs = client.approximateStreamPosition.coerceAtLeast(0L),
                durationMs = duration
            )
        }
    }
}
