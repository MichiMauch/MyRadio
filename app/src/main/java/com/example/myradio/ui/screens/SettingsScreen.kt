package com.example.myradio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myradio.ui.theme.StereoAmber
import com.example.myradio.ui.theme.StereoOutline
import com.example.myradio.ui.theme.StereoPanel
import com.example.myradio.ui.theme.StereoPanelRaised
import com.example.myradio.viewmodel.SettingsUiState
import com.example.myradio.viewmodel.ViewMode

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: SettingsUiState,
    onStationViewModeChange: (ViewMode) -> Unit,
    onPodcastViewModeChange: (ViewMode) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ansicht",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ViewModeRow(
                label = "Sender-Ansicht",
                currentMode = state.stationViewMode,
                onModeChange = onStationViewModeChange
            )
        }

        item {
            ViewModeRow(
                label = "Podcast-Ansicht",
                currentMode = state.podcastViewMode,
                onModeChange = onPodcastViewModeChange
            )
        }
    }
}

@Composable
private fun ViewModeRow(
    label: String,
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ViewMode.entries.forEach { mode ->
                val selected = mode == currentMode
                Button(
                    onClick = { onModeChange(mode) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) StereoPanelRaised else StereoPanel,
                        contentColor = if (selected) StereoAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) StereoAmber else StereoOutline
                    )
                ) {
                    Text(
                        text = when (mode) {
                            ViewMode.GRID -> "Kacheln"
                            ViewMode.LIST -> "Liste"
                        }
                    )
                }
            }
        }
    }
}
