package com.example.myradio.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.myradio.data.model.RadioStation
import com.example.myradio.ui.theme.StereoAmber
import com.example.myradio.ui.theme.StereoText
import com.example.myradio.ui.theme.StereoOutline
import com.example.myradio.ui.theme.StereoPanel
import com.example.myradio.ui.theme.StereoPanelRaised

@Composable
fun StationItem(
    station: RadioStation,
    isCurrentStation: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onToggleFavorite: () -> Unit = {},
    showDragHandle: Boolean = true,
    dragModifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Sender löschen") },
            text = { Text("\"${station.name}\" wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    val containerColor = if (isCurrentStation) {
        StereoPanelRaised
    } else {
        StereoPanel
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LED indicator
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isCurrentStation) StereoAmber else StereoOutline)
            )

            if (showDragHandle) {
                // Drag handle
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Reihenfolge ändern",
                    modifier = dragModifier.size(24.dp),
                    tint = StereoText
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Station logo or fallback radio icon
            if (station.logoUrl.isNotBlank()) {
                Log.d("StationItem", "Loading logo for ${station.name}: ${station.logoUrl}")
                SubcomposeAsyncImage(
                    model = station.logoUrl,
                    contentDescription = station.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    error = {
                        Log.e("StationItem", "Failed to load logo for ${station.name}: ${station.logoUrl}")
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    success = {
                        SubcomposeAsyncImageContent()
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (isCurrentStation) StereoAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (station.genre.isNotEmpty()) {
                    Text(
                        text = station.genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (station.country.isNotEmpty()) {
                    Text(
                        text = station.country,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Favorite button
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (station.isFavorite) "Aus Favoriten entfernen" else "Zu Favoriten",
                    tint = if (station.isFavorite) StereoAmber else StereoText
                )
            }

            // Play/Pause button
            IconButton(onClick = onClick) {
                if (isCurrentStation && isPlaying) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = StereoAmber
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = StereoAmber
                    )
                }
            }

            if (onDelete != null) {
                // Delete button
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = StereoText
                    )
                }
            }
        }
    }
}
