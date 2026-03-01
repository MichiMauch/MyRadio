package com.example.myradio

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.cast.framework.CastContext
import com.example.myradio.ui.screens.RadioApp
import com.example.myradio.ui.theme.MyRadioTheme
import com.example.myradio.viewmodel.CatalogViewModel
import com.example.myradio.viewmodel.MyRadioViewModelFactory
import com.example.myradio.viewmodel.PlaybackViewModel
import com.example.myradio.viewmodel.PodcastViewModel
import com.example.myradio.viewmodel.SettingsViewModel
import com.example.myradio.viewmodel.StationViewModel

class MainActivity : FragmentActivity() {

    private val factory by lazy { MyRadioViewModelFactory(application as MyRadioApplication) }

    private val playbackViewModel: PlaybackViewModel by viewModels { factory }
    private val stationViewModel: StationViewModel by viewModels { factory }
    private val catalogViewModel: CatalogViewModel by viewModels { factory }
    private val podcastViewModel: PodcastViewModel by viewModels { factory }
    private val settingsViewModel: SettingsViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyRadioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RadioApp(
                        playbackViewModel = playbackViewModel,
                        stationViewModel = stationViewModel,
                        catalogViewModel = catalogViewModel,
                        podcastViewModel = podcastViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        playbackViewModel.connectToService(this)
        playbackViewModel.connectToCast()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return CastContext.getSharedInstance(this)
            .onDispatchVolumeKeyEventBeforeJellyBean(event)
                || super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        super.onStop()
        playbackViewModel.disconnectFromCast()
        playbackViewModel.disconnectFromService()
    }
}
