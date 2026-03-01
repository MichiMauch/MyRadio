package com.example.myradio.ui.screens

import android.view.ContextThemeWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import androidx.mediarouter.R as MediaRouterR
import com.example.myradio.playback.PlaybackRoute
import com.example.myradio.ui.components.NowPlayingBar
import com.example.myradio.ui.theme.StereoAmber
import com.example.myradio.ui.theme.StereoOutline
import com.example.myradio.ui.theme.StereoPanel
import com.example.myradio.ui.theme.StereoPanelRaised
import com.example.myradio.viewmodel.CatalogViewModel
import com.example.myradio.viewmodel.PlaybackViewModel
import com.example.myradio.viewmodel.PodcastViewModel
import com.example.myradio.viewmodel.SettingsViewModel
import com.example.myradio.viewmodel.StationViewModel
import com.example.myradio.viewmodel.ViewMode
import com.google.android.gms.cast.framework.CastButtonFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioApp(
    playbackViewModel: PlaybackViewModel,
    stationViewModel: StationViewModel,
    catalogViewModel: CatalogViewModel,
    podcastViewModel: PodcastViewModel,
    settingsViewModel: SettingsViewModel
) {
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val stations by stationViewModel.stations.collectAsState()
    val songHistory by playbackViewModel.songHistory.collectAsState()
    val catalogState by catalogViewModel.catalogState.collectAsState()
    val podcastState by podcastViewModel.podcastState.collectAsState()
    val discoveryState by podcastViewModel.discoveryState.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showHistory by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedStationId by remember { mutableStateOf<Int?>(null) }
    var isSortMode by remember { mutableStateOf(false) }

    if (showHistory) {
        SongHistorySheet(
            history = songHistory,
            onDismiss = { showHistory = false },
            onClearHistory = { playbackViewModel.clearHistory() }
        )
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) {
            podcastViewModel.loadDiscovery()
        }
        if (selectedTab != 0) {
            selectedStationId = null
            isSortMode = false
        }
    }

    BackHandler(enabled = selectedTab == 0 && selectedStationId != null) {
        selectedStationId = null
    }
    BackHandler(enabled = selectedTab == 0 && isSortMode) {
        isSortMode = false
    }
    BackHandler(enabled = selectedTab == 4) {
        selectedTab = 0
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedTab) {
                            0 -> "MyRadio"
                            1 -> "Sender-Katalog"
                            2 -> "Podcasts"
                            4 -> "Einstellungen"
                            else -> "Entdecken"
                        }
                    )
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { selectedTab = 4 }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Einstellungen"
                        )
                    }
                    CastActionButton()
                    if (selectedTab == 0 && selectedStationId == null && !isSortMode) {
                        IconButton(onClick = { selectedTab = 1 }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Sender suchen"
                            )
                        }
                        IconButton(onClick = { isSortMode = true }) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Sender sortieren"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                val hasActiveMedia = playbackState.currentStation != null ||
                    !playbackState.currentTitle.isNullOrBlank() ||
                    playbackState.isPlaying ||
                    playbackState.isBuffering ||
                    playbackState.isRetrying
                if (hasActiveMedia) {
                    NowPlayingBar(
                        stationName = playbackState.currentStation?.name
                            ?: playbackState.currentTitle
                            ?: "",
                        genre = playbackState.currentStation?.genre
                            ?: playbackState.currentSubtitle
                            ?: "",
                        logoUrl = playbackState.currentStation?.logoUrl
                            ?: playbackState.currentArtworkUrl
                            ?: "",
                        outputRouteLabel = if (playbackState.outputRoute == PlaybackRoute.CAST) {
                            "Ausgabe: ${playbackState.castDeviceName ?: "Google Nest"}"
                        } else {
                            "Ausgabe: Handy"
                        },
                        nowPlayingTitle = playbackState.nowPlayingTitle,
                        currentPositionMs = playbackState.currentPositionMs,
                        durationMs = playbackState.durationMs,
                        isPlaying = playbackState.isPlaying,
                        isBuffering = playbackState.isBuffering,
                        isRetrying = playbackState.isRetrying,
                        retryAttempt = playbackState.retryAttempt,
                        retryMaxAttempts = playbackState.retryMaxAttempts,
                        lastError = playbackState.lastError,
                        onPlayPauseClick = { playbackViewModel.togglePlayPause() }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 0.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val senderSelected = selectedTab == 0
                    Button(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .shadow(
                                elevation = if (senderSelected) 10.dp else 2.dp,
                                ambientColor = StereoAmber,
                                spotColor = StereoAmber
                            )
                            .heightIn(min = 64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (senderSelected) StereoPanelRaised else StereoPanel,
                            contentColor = if (senderSelected) StereoAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (senderSelected) StereoAmber else StereoOutline
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Sender",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Button(
                        onClick = { showHistory = true },
                        modifier = Modifier
                            .weight(1f)
                            .shadow(
                                elevation = 2.dp,
                                ambientColor = StereoAmber,
                                spotColor = StereoAmber
                            )
                            .heightIn(min = 64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StereoPanel,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StereoOutline)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Verlauf",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    val podcastSelected = selectedTab == 2
                    Button(
                        onClick = { selectedTab = 2 },
                        modifier = Modifier
                            .weight(1f)
                            .shadow(
                                elevation = if (podcastSelected) 10.dp else 2.dp,
                                ambientColor = StereoAmber,
                                spotColor = StereoAmber
                            )
                            .heightIn(min = 64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (podcastSelected) StereoPanelRaised else StereoPanel,
                            contentColor = if (podcastSelected) StereoAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (podcastSelected) StereoAmber else StereoOutline
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Podcasts",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    val discoverSelected = selectedTab == 3
                    Button(
                        onClick = { selectedTab = 3 },
                        modifier = Modifier
                            .weight(1f)
                            .shadow(
                                elevation = if (discoverSelected) 10.dp else 2.dp,
                                ambientColor = StereoAmber,
                                spotColor = StereoAmber
                            )
                            .heightIn(min = 64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (discoverSelected) StereoPanelRaised else StereoPanel,
                            contentColor = if (discoverSelected) StereoAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (discoverSelected) StereoAmber else StereoOutline
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Entdecken",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (selectedTab == 0) {
            val detailStation = stations.firstOrNull { it.id == selectedStationId }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                when {
                    isSortMode -> {
                        StationSortPage(
                            stations = stations,
                            onReorder = { reordered -> stationViewModel.updateStationOrder(reordered) }
                        )
                    }
                    detailStation != null -> {
                        val isCurrent = detailStation.id == playbackState.currentStation?.id
                        StationDetailPage(
                            station = detailStation,
                            isCurrentStation = isCurrent,
                            isPlaying = isCurrent && playbackState.isPlaying,
                            onPlayPause = {
                                if (isCurrent) {
                                    playbackViewModel.togglePlayPause()
                                } else {
                                    playbackViewModel.playStation(detailStation)
                                }
                            },
                            onToggleFavorite = { stationViewModel.toggleFavorite(detailStation.id) },
                            onDelete = {
                                if (playbackState.currentStation?.id == detailStation.id) {
                                    playbackViewModel.stop()
                                }
                                stationViewModel.deleteStation(detailStation.id)
                                selectedStationId = null
                            }
                        )
                    }
                    else -> {
                        StationsGridPage(
                            stations = stations,
                            currentStationId = playbackState.currentStation?.id,
                            isPlaying = playbackState.isPlaying,
                            viewMode = settingsState.stationViewMode,
                            onTileClick = { station ->
                                if (station.id == playbackState.currentStation?.id) {
                                    playbackViewModel.togglePlayPause()
                                } else {
                                    playbackViewModel.playStation(station)
                                }
                            },
                            onOpenDetail = { stationId ->
                                selectedStationId = stationId
                            },
                            onDelete = { stationId ->
                                if (playbackState.currentStation?.id == stationId) {
                                    playbackViewModel.stop()
                                }
                                stationViewModel.deleteStation(stationId)
                            }
                        )
                    }
                }
            }
        } else if (selectedTab == 1) {
            RadioCatalogScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                state = catalogState,
                onQueryChange = { catalogViewModel.updateCatalogQuery(it) },
                onSearch = { catalogViewModel.searchCatalog() },
                onAddStation = { catalogViewModel.addCatalogStation(it) }
            )
        } else if (selectedTab == 2) {
            PodcastsScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                state = podcastState,
                viewMode = settingsState.podcastViewMode,
                onFeedUrlChange = { podcastViewModel.updatePodcastFeedUrl(it) },
                onAddFeed = { podcastViewModel.addPodcastSubscription() },
                onOpenSearch = { podcastViewModel.openPodcastSearch() },
                onCloseSearch = { podcastViewModel.closePodcastSearch() },
                onSearchQueryChange = { podcastViewModel.updatePodcastSearchQuery(it) },
                onSearchPodcasts = { podcastViewModel.searchPodcasts() },
                onSubscribeFromSearch = { result -> podcastViewModel.subscribeFromSearch(result) },
                onRefresh = { podcastViewModel.refreshPodcastEpisodes() },
                onSelectFeed = { podcastViewModel.selectPodcastSubscription(it) },
                onSelectEpisode = { podcastViewModel.selectPodcastEpisode(it) },
                onRemoveFeed = { podcastViewModel.removePodcastSubscription(it) },
                onPlayEpisode = { episode ->
                    val podcastImageUrl = podcastState.subscriptions
                        .firstOrNull { it.feedUrl.equals(episode.feedUrl, ignoreCase = true) }
                        ?.imageUrl
                        .orEmpty()
                    playbackViewModel.togglePodcastEpisode(episode, podcastImageUrl)
                },
                playbackState = playbackState
            )
        } else if (selectedTab == 4) {
            SettingsScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                state = settingsState,
                onStationViewModeChange = { settingsViewModel.setStationViewMode(it) },
                onPodcastViewModeChange = { settingsViewModel.setPodcastViewMode(it) }
            )
        } else {
            PodcastDiscoveryScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                state = discoveryState,
                onRefresh = { podcastViewModel.refreshDiscovery() },
                onSubscribe = { itemId -> podcastViewModel.subscribeFromDiscovery(itemId) }
            )
        }
    }
}

@Composable
private fun CastActionButton() {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            val themedContext = ContextThemeWrapper(ctx, MediaRouterR.style.Theme_MediaRouter)
            MediaRouteButton(themedContext).apply {
                contentDescription = "Cast"
                runCatching {
                    CastButtonFactory.setUpMediaRouteButton(themedContext, this)
                }
            }
        },
        update = { button ->
            runCatching {
                CastButtonFactory.setUpMediaRouteButton(context, button)
            }
        },
        modifier = Modifier.size(40.dp)
    )
}
