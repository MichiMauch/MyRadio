package com.example.myradio.playback

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.myradio.data.local.StationsDataSource
import com.example.myradio.data.local.StationsJsonStorage
import com.example.myradio.playback.VisualizerManager
import com.example.myradio.widget.RadioWidgetProvider
import com.example.myradio.widget.WidgetState
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var lastIcyTitle: String? = null

    companion object {
        private const val TAG = "PlaybackService"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Creating PlaybackService")

        // HTTP DataSource die Redirects (302) folgt
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Debug listener
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateStr = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($playbackState)"
                }
                Log.d(TAG, "Player state: $stateStr")
                if (playbackState == Player.STATE_READY) {
                    VisualizerManager.start(player.audioSessionId)
                }
                if (playbackState == Player.STATE_IDLE) {
                    updateWidget()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "isPlaying: $isPlaying")
                if (isPlaying) {
                    VisualizerManager.start(player.audioSessionId)
                }
                updateWidget()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                VisualizerManager.start(audioSessionId)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error: ${error.errorCodeName} - ${error.message}", error)
            }

            // ICY metadata (Shoutcast/Icecast stream title) arrives here
            override fun onMetadata(metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    val entry = metadata.get(i)
                    Log.d(TAG, "onMetadata entry: ${entry.javaClass.simpleName} -> $entry")

                    if (entry is IcyInfo) {
                        val icyTitle = entry.title
                        Log.d(TAG, "ICY title: '$icyTitle'")

                        if (!icyTitle.isNullOrBlank() && icyTitle != lastIcyTitle) {
                            lastIcyTitle = icyTitle

                            // Update the MediaItem metadata so both the notification
                            // and MediaController clients see the current track
                            val currentItem = player.currentMediaItem ?: return
                            val existingMeta = currentItem.mediaMetadata

                            val stationName = existingMeta.station
                                ?: existingMeta.displayTitle
                                ?: existingMeta.artist

                            val updatedMetadata = MediaMetadata.Builder()
                                .populate(existingMeta)
                                // Notification: title line = station name
                                .setDisplayTitle(stationName)
                                // Notification: subtitle line = current song
                                .setSubtitle(icyTitle)
                                .setArtist(icyTitle)
                                // Keep station in station field
                                .setStation(stationName)
                                .build()

                            val updatedItem = currentItem.buildUpon()
                                .setMediaMetadata(updatedMetadata)
                                .build()

                            player.replaceMediaItem(
                                player.currentMediaItemIndex,
                                updatedItem
                            )
                            Log.d(TAG, "Updated metadata: station='$stationName', track='$icyTitle'")
                            updateWidget()
                        }
                    }
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: List<MediaItem>
                ): ListenableFuture<List<MediaItem>> {
                    // Reset ICY title tracking for new station
                    lastIcyTitle = null

                    // When MediaController sends MediaItems via setMediaItem(),
                    // the URI gets stripped and placed in requestMetadata.
                    // We must rebuild the items with the actual URI here.
                    val resolvedItems = mediaItems.map { item ->
                        if (item.localConfiguration != null) {
                            item
                        } else {
                            item.buildUpon()
                                .setUri(item.requestMetadata.mediaUri)
                                .build()
                        }
                    }
                    Log.d(TAG, "onAddMediaItems: resolved ${resolvedItems.size} items")
                    resolvedItems.forEach {
                        Log.d(TAG, "  -> id=${it.mediaId} uri=${it.localConfiguration?.uri}")
                    }
                    return Futures.immediateFuture(resolvedItems)
                }

                // Handle Bluetooth/system resume when no media is loaded
                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    val ctx = this@PlaybackService
                    val lastId = StationsJsonStorage.loadLastPlayedStationId(ctx)
                    val deletedIds = StationsJsonStorage.loadDeletedDefaultIds(ctx)
                    val allStations = StationsDataSource.getStations()
                        .filter { it.id !in deletedIds } +
                        StationsJsonStorage.loadUserStations(ctx)

                    val station = allStations.find { it.id == lastId }
                        ?: allStations.firstOrNull()

                    if (station != null) {
                        lastIcyTitle = null
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

                        Log.d(TAG, "onPlaybackResumption: resuming ${station.name}")
                        return Futures.immediateFuture(
                            MediaSession.MediaItemsWithStartPosition(
                                listOf(mediaItem), 0, C.TIME_UNSET
                            )
                        )
                    }

                    Log.w(TAG, "onPlaybackResumption: no stations available")
                    return Futures.immediateFailedFuture(
                        UnsupportedOperationException("No stations available")
                    )
                }
            })
            .build()

        Log.d(TAG, "onCreate: MediaSession created successfully")
    }

    private fun updateWidget() {
        val player = mediaSession?.player ?: return
        val metadata = player.currentMediaItem?.mediaMetadata
        RadioWidgetProvider.updateAllWidgets(
            this,
            WidgetState(
                stationName = metadata?.station?.toString()
                    ?: metadata?.displayTitle?.toString(),
                nowPlayingTitle = metadata?.subtitle?.toString(),
                isPlaying = player.isPlaying
            )
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        RadioWidgetProvider.updateAllWidgets(this, WidgetState())
        VisualizerManager.stop()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
