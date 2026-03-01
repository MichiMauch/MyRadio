package com.example.myradio.viewmodel

import androidx.lifecycle.ViewModel
import com.example.myradio.data.model.RadioStation
import com.example.myradio.data.repository.RadioRepository
import kotlinx.coroutines.flow.StateFlow

class StationViewModel(
    private val repository: RadioRepository
) : ViewModel() {

    val stations: StateFlow<List<RadioStation>> = repository.stations

    fun toggleFavorite(stationId: Int) {
        repository.toggleFavorite(stationId)
    }

    fun updateStationOrder(stations: List<RadioStation>) {
        repository.updateStationOrder(stations)
    }

    fun addStation(name: String, streamUrl: String, genre: String, country: String, logoUrl: String = "") {
        repository.addStation(name, streamUrl, genre, country, logoUrl)
    }

    fun deleteStation(id: Int) {
        repository.deleteStation(id)
    }
}
