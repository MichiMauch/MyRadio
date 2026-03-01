package com.example.myradio.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.myradio.data.model.RadioStation
import com.example.myradio.ui.components.StationItem
import com.example.myradio.ui.theme.StereoAmber
import com.example.myradio.ui.theme.StereoOutline
import com.example.myradio.viewmodel.ViewMode
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationsGridPage(
    stations: List<RadioStation>,
    currentStationId: Int?,
    isPlaying: Boolean,
    viewMode: ViewMode = ViewMode.GRID,
    onTileClick: (RadioStation) -> Unit,
    onOpenDetail: (Int) -> Unit,
    onDelete: (Int) -> Unit = {}
) {
    var deleteStation by remember { mutableStateOf<RadioStation?>(null) }

    deleteStation?.let { station ->
        AlertDialog(
            onDismissRequest = { deleteStation = null },
            title = { Text("Sender löschen") },
            text = { Text("\"${station.name}\" wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(station.id)
                    deleteStation = null
                }) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteStation = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (viewMode == ViewMode.LIST) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp)
        ) {
            items(stations, key = { it.id }) { station ->
                val isCurrent = station.id == currentStationId
                StationItem(
                    station = station,
                    isCurrentStation = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onClick = { onTileClick(station) },
                    onDelete = null,
                    showDragHandle = false
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(stations, key = { it.id }) { station ->
                val isCurrent = station.id == currentStationId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .combinedClickable(
                            onClick = { onTileClick(station) },
                            onLongClick = { onOpenDetail(station.id) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = if (isCurrent && isPlaying) StereoAmber else StereoOutline
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (station.logoUrl.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = station.logoUrl,
                                contentDescription = station.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    StationTileFallback(
                                        station = station,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                },
                                error = {
                                    StationTileFallback(
                                        station = station,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                },
                                success = { SubcomposeAsyncImageContent() }
                            )
                        } else {
                            StationTileFallback(
                                station = station,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        StationTileNameOverlay(
                            name = station.name,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        // Delete icon top-right
                        IconButton(
                            onClick = { deleteStation = station },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Löschen",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationTileFallback(
    station: RadioStation,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant
    )
    val bg = palette[kotlin.math.abs(station.id) % palette.size]
    val initials = stationInitials(station.name)

    Box(
        modifier = modifier.background(bg.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StationTileNameOverlay(
    name: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun stationInitials(name: String): String {
    val words = name
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    if (words.isEmpty()) return "?"
    return when {
        words.size == 1 -> words.first().take(2)
        else -> "${words[0].first()}${words[1].first()}"
    }.uppercase()
}

@Composable
fun StationDetailPage(
    station: RadioStation,
    isCurrentStation: Boolean,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Sender löschen") },
            text = { Text("\"${station.name}\" wirklich löschen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .aspectRatio(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (station.logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = station.logoUrl,
                            contentDescription = station.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (station.genre.isNotBlank()) {
                Text(
                    text = station.genre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            if (station.country.isNotBlank()) {
                Text(
                    text = station.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onPlayPause, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isCurrentStation && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isCurrentStation && isPlaying) "Pausieren" else "Abspielen")
                }
                Button(onClick = onToggleFavorite, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (station.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (station.isFavorite) "Favorit" else "Favorisieren")
                }
            }

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sender löschen")
            }
        }
    }
}

@Composable
fun StationSortPage(
    stations: List<RadioStation>,
    onReorder: (List<RadioStation>) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val mutable = stations.toMutableList()
        val moved = mutable.removeAt(from.index)
        mutable.add(to.index, moved)
        onReorder(mutable)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Sender sortieren",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ziehe einen Sender am Griff, um die Reihenfolge zu ändern.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(stations, key = { it.id }) { station ->
            ReorderableItem(reorderableState, key = station.id) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Sortieren",
                            modifier = Modifier
                                .draggableHandle()
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (station.logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = station.logoUrl,
                                contentDescription = station.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radio,
                                    contentDescription = null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (station.genre.isNotBlank()) {
                                Text(
                                    text = station.genre,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
