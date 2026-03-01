package com.example.myradio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myradio.data.model.RadioStation
import com.example.myradio.data.repository.RadioRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StationViewModel(
    private val repository: RadioRepository
) : ViewModel() {

    val stations: StateFlow<List<RadioStation>> = repository.stations

    fun toggleFavorite(stationId: Int) {
        viewModelScope.launch { repository.toggleFavorite(stationId) }
    }

    fun updateStationOrder(stations: List<RadioStation>) {
        viewModelScope.launch { repository.updateStationOrder(stations) }
    }

    fun addStation(name: String, streamUrl: String, genre: String, country: String, logoUrl: String = "") {
        viewModelScope.launch { repository.addStation(name, streamUrl, genre, country, logoUrl) }
    }

    fun deleteStation(id: Int) {
        viewModelScope.launch { repository.deleteStation(id) }
    }
}
