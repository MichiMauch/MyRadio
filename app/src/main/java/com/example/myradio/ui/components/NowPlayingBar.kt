package com.example.myradio.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.myradio.ui.theme.StereoAmber
import com.example.myradio.ui.theme.StereoBlack
import com.example.myradio.ui.theme.StereoOutline
import com.example.myradio.ui.theme.StereoPanel
import com.example.myradio.ui.theme.StereoPanelRaised
import com.example.myradio.ui.theme.StereoSubtext
import com.example.myradio.ui.theme.StereoText

@Composable
fun NowPlayingBar(
    stationName: String,
    genre: String,
    logoUrl: String,
    outputRouteLabel: String,
    nowPlayingTitle: String?,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPauseClick: () -> Unit
) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        color = StereoBlack,
        border = BorderStroke(1.dp, StereoOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
        ) {
            if (isBuffering) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = StereoAmber,
                    trackColor = StereoPanel
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val vfdTitleStyle = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    shadow = Shadow(
                        color = StereoAmber,
                        offset = Offset(0f, 0f),
                        blurRadius = 10f
                    )
                )
                val vfdSubStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp,
                    shadow = Shadow(
                        color = StereoAmber,
                        offset = Offset(0f, 0f),
                        blurRadius = 6f
                    )
                )

                // Station logo
                if (logoUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = logoUrl,
                        contentDescription = stationName,
                        modifier = Modifier
                            .size(52.dp)
                            .padding(2.dp),
                        contentScale = ContentScale.Fit,
                        error = {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = StereoSubtext
                            )
                        },
                        success = { SubcomposeAsyncImageContent() }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = StereoSubtext
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text(
                        text = stationName,
                        style = vfdTitleStyle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = StereoText,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1000,
                            velocity = 28.dp
                        )
                    )
                    if (!nowPlayingTitle.isNullOrBlank()) {
                        Text(
                            text = nowPlayingTitle,
                            style = vfdSubStyle,
                            color = StereoAmber,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 2000,
                                velocity = 30.dp
                            )
                        )
                    }
                    Text(
                        text = if (isBuffering) "Buffering..." else genre,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.6.sp
                        ),
                        color = StereoSubtext,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatPlaybackTime(currentPositionMs, durationMs)}  •  $outputRouteLabel",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.4.sp
                        ),
                        color = StereoAmber,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StereoPanelRaised),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = StereoAmber
                    )
                }

            }
        }
    }
}

private fun formatPlaybackTime(positionMs: Long, durationMs: Long): String {
    val pos = positionMs.coerceAtLeast(0L)
    return if (durationMs > 0L) {
        "${toClock(pos)} / ${toClock(durationMs)}"
    } else {
        "${toClock(pos)} / LIVE"
    }
}

private fun toClock(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
