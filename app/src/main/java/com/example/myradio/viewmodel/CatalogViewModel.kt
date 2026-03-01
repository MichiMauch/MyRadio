package com.example.myradio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myradio.data.model.RadioBrowserStation
import com.example.myradio.data.remote.RadioBrowserApi
import com.example.myradio.data.repository.RadioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CatalogViewModel(
    private val repository: RadioRepository
) : ViewModel() {

    private val _catalogState = MutableStateFlow(CatalogUiState())
    val catalogState: StateFlow<CatalogUiState> = _catalogState.asStateFlow()

    fun updateCatalogQuery(query: String) {
        _catalogState.update { it.copy(query = query, errorMessage = null, lastAddedName = null) }
    }

    fun searchCatalog() {
        val query = _catalogState.value.query.trim()
        if (query.isBlank()) {
            _catalogState.update {
                it.copy(results = emptyList(), isLoading = false, errorMessage = "Bitte Suchbegriff eingeben")
            }
            return
        }

        _catalogState.update { it.copy(isLoading = true, errorMessage = null, lastAddedName = null) }

        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                RadioBrowserApi.searchStations(query).mapNotNull { dto ->
                    val url = dto.urlResolved.trim()
                    val name = dto.name.trim()
                    if (name.isBlank() || url.isBlank()) return@mapNotNull null

                    RadioBrowserStation(
                        name = name,
                        streamUrl = url,
                        genre = dto.tags.toGenre(),
                        country = dto.countryCode.trim(),
                        logoUrl = dto.favicon.trim()
                    )
                }
            }

            _catalogState.update {
                it.copy(
                    results = results,
                    isLoading = false,
                    errorMessage = if (results.isEmpty()) "Keine Treffer" else null
                )
            }
        }
    }

    fun addCatalogStation(station: RadioBrowserStation) {
        val exists = repository.stations.value.any {
            it.streamUrl.equals(station.streamUrl, ignoreCase = true) ||
                it.name.equals(station.name, ignoreCase = true)
        }
        if (exists) {
            _catalogState.update { it.copy(errorMessage = "Sender bereits vorhanden", lastAddedName = null) }
            return
        }

        viewModelScope.launch {
            repository.addStation(
                name = station.name,
                streamUrl = station.streamUrl,
                genre = station.genre,
                country = station.country,
                logoUrl = station.logoUrl
            )
            _catalogState.update { it.copy(lastAddedName = station.name, errorMessage = null) }
        }
    }
}

private fun String.toGenre(): String {
    if (this.isBlank()) return ""
    return split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString(" / ")
}
