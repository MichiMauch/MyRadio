package com.example.myradio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myradio.MyRadioApplication

class MyRadioViewModelFactory(
    private val app: MyRadioApplication
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(PlaybackViewModel::class.java) ->
            PlaybackViewModel(app.radioRepository, app, app.castManager)
        modelClass.isAssignableFrom(StationViewModel::class.java) ->
            StationViewModel(app.radioRepository)
        modelClass.isAssignableFrom(CatalogViewModel::class.java) ->
            CatalogViewModel(app.radioRepository)
        modelClass.isAssignableFrom(PodcastViewModel::class.java) ->
            PodcastViewModel(app.podcastRepository)
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    } as T
}
