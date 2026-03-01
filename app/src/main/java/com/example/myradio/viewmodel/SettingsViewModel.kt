package com.example.myradio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myradio.data.local.db.AppSettingEntity
import com.example.myradio.data.local.db.AppSettingsDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ViewMode { GRID, LIST }

data class SettingsUiState(
    val stationViewMode: ViewMode = ViewMode.GRID,
    val podcastViewMode: ViewMode = ViewMode.GRID
)

class SettingsViewModel(
    private val settingsDao: AppSettingsDao
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        viewModelScope.launch {
            val stationMode = settingsDao.getValue("station_view_mode")
                ?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() }
                ?: ViewMode.GRID
            val podcastMode = settingsDao.getValue("podcast_view_mode")
                ?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() }
                ?: ViewMode.GRID
            _state.value = SettingsUiState(stationMode, podcastMode)
        }
    }

    fun setStationViewMode(mode: ViewMode) {
        _state.value = _state.value.copy(stationViewMode = mode)
        viewModelScope.launch {
            settingsDao.upsert(AppSettingEntity("station_view_mode", mode.name))
        }
    }

    fun setPodcastViewMode(mode: ViewMode) {
        _state.value = _state.value.copy(podcastViewMode = mode)
        viewModelScope.launch {
            settingsDao.upsert(AppSettingEntity("podcast_view_mode", mode.name))
        }
    }
}
