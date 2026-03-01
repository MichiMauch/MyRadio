package com.example.myradio.viewmodel

import com.example.myradio.data.model.RadioBrowserStation

data class CatalogUiState(
    val query: String = "",
    val results: List<RadioBrowserStation> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastAddedName: String? = null
)
