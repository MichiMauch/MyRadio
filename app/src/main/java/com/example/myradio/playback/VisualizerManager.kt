package com.example.myradio.playback

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

object VisualizerManager {

    private const val TAG = "VisualizerManager"

    private val _waveform = MutableStateFlow<List<Float>>(emptyList())
    val waveform: StateFlow<List<Float>> = _waveform

    private var visualizer: Visualizer? = null
    private var sessionId: Int = 0

    fun start(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId == sessionId && visualizer != null) return

        stop()
        sessionId = audioSessionId

        try {
            val v = Visualizer(audioSessionId)
            val range = Visualizer.getCaptureSizeRange()
            v.captureSize = range[1]
            v.scalingMode = Visualizer.SCALING_MODE_AS_PLAYED
            v.measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer,
                        waveform: ByteArray,
                        samplingRate: Int
                    ) {
                        if (waveform.isEmpty()) return
                        val bucketSize = 64
                        val step = (waveform.size / bucketSize).coerceAtLeast(1)
                        val samples = ArrayList<Float>(bucketSize)
                        var i = 0
                        while (i < waveform.size) {
                            // waveform byte: 0..255 mapped around 128
                            val value = (waveform[i].toInt() and 0xFF) - 128
                            val normalized = (value / 128f).coerceIn(-1f, 1f)
                            samples.add(normalized)
                            i += step
                        }
                        // Normalize quiet streams a bit so line is visible
                        val maxAbs = samples.maxOfOrNull { abs(it) } ?: 1f
                        val scaled = if (maxAbs < 0.15f) {
                            samples.map { it / 0.15f }
                        } else {
                            samples
                        }
                        _waveform.value = scaled
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer,
                        fft: ByteArray,
                        samplingRate: Int
                    ) = Unit
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                false
            )
            v.enabled = true
            visualizer = v
            Log.d(TAG, "Visualizer started for session=$audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Visualizer start failed", e)
            stop()
        }
    }

    fun stop() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {
        } finally {
            visualizer = null
            sessionId = 0
            _waveform.value = emptyList()
        }
    }
}
