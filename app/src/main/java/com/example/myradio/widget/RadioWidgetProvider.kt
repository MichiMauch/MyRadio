package com.example.myradio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.myradio.MainActivity
import com.example.myradio.R
import com.example.myradio.playback.PlaybackService
import com.google.common.util.concurrent.MoreExecutors

class RadioWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "RadioWidgetProvider"
        const val ACTION_PLAY_PAUSE = "com.example.myradio.WIDGET_PLAY_PAUSE"
        const val ACTION_STOP = "com.example.myradio.WIDGET_STOP"

        fun updateAllWidgets(context: Context, state: WidgetState) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, RadioWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            val views = buildViews(context, state)
            for (id in ids) {
                manager.updateAppWidget(id, views)
            }
            Log.d(TAG, "Updated ${ids.size} widgets: station='${state.stationName}' playing=${state.isPlaying}")
        }

        private fun buildViews(context: Context, state: WidgetState): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_now_playing)

            views.setTextViewText(
                R.id.widget_station_name,
                state.stationName ?: "MyRadio"
            )
            views.setTextViewText(
                R.id.widget_now_playing,
                state.nowPlayingTitle ?: if (state.stationName != null) "" else "Kein Sender"
            )
            // Activate marquee scrolling for the now-playing text
            views.setBoolean(R.id.widget_now_playing, "setSelected", true)

            views.setImageViewResource(
                R.id.widget_play_pause,
                if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )

            // Play/Pause button
            val playPauseIntent = Intent(context, RadioWidgetProvider::class.java)
                .setAction(ACTION_PLAY_PAUSE)
            views.setOnClickPendingIntent(
                R.id.widget_play_pause,
                PendingIntent.getBroadcast(
                    context, 0, playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Stop button
            val stopIntent = Intent(context, RadioWidgetProvider::class.java)
                .setAction(ACTION_STOP)
            views.setOnClickPendingIntent(
                R.id.widget_stop,
                PendingIntent.getBroadcast(
                    context, 1, stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Tap body -> open app
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_body,
                PendingIntent.getActivity(
                    context, 2, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            return views
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate: ${appWidgetIds.size} widgets")
        updateAllWidgets(context, WidgetState())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                Log.d(TAG, "Widget: Play/Pause pressed")
                sendMediaCommand(context, isPlayPause = true)
            }
            ACTION_STOP -> {
                Log.d(TAG, "Widget: Stop pressed")
                sendMediaCommand(context, isPlayPause = false)
            }
        }
    }

    private fun sendMediaCommand(context: Context, isPlayPause: Boolean) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            try {
                val controller = future.get()
                if (isPlayPause) {
                    if (controller.isPlaying) {
                        controller.pause()
                    } else {
                        controller.play()
                    }
                } else {
                    controller.stop()
                    controller.clearMediaItems()
                }
                MediaController.releaseFuture(future)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending media command", e)
            }
        }, MoreExecutors.directExecutor())
    }
}
