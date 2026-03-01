package com.example.myradio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myradio.data.model.PodcastEpisode
import com.example.myradio.data.model.PodcastSearchResult
import com.example.myradio.playback.PlaybackUiState
import com.example.myradio.viewmodel.PodcastUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PodcastsScreen(
    modifier: Modifier = Modifier,
    state: PodcastUiState,
    onFeedUrlChange: (String) -> Unit,
    onAddFeed: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchPodcasts: () -> Unit,
    onSubscribeFromSearch: (PodcastSearchResult) -> Unit,
    onRefresh: () -> Unit,
    onSelectFeed: (Long?) -> Unit,
    onSelectEpisode: (String?) -> Unit,
    onRemoveFeed: (Long) -> Unit,
    onPlayEpisode: (PodcastEpisode) -> Unit,
    playbackState: PlaybackUiState
) {
    if (state.isSearchPageOpen) {
        BackHandler { onCloseSearch() }
        PodcastSearchPage(
            modifier = modifier,
            state = state,
            onSearchQueryChange = onSearchQueryChange,
            onSearchPodcasts = onSearchPodcasts,
            onSubscribeFromSearch = onSubscribeFromSearch,
            onFeedUrlChange = onFeedUrlChange,
            onAddFeed = onAddFeed
        )
        return
    }

    val selectedSubscription = state.subscriptions.firstOrNull { it.id == state.selectedSubscriptionId }
    val episodesByFeed = state.episodes.groupBy { normalizeFeedUrl(it.feedUrl) }
    val episodesForSelectedPodcast = selectedSubscription?.let { sub ->
        episodesByFeed[normalizeFeedUrl(sub.feedUrl)].orEmpty().ifEmpty {
            state.episodes.filter { it.podcastTitle.equals(sub.title, ignoreCase = true) }
        }
    }.orEmpty()
    val selectedEpisode = episodesForSelectedPodcast.firstOrNull { it.id == state.selectedEpisodeId }

    BackHandler(enabled = selectedEpisode != null) {
        onSelectEpisode(null)
    }
    BackHandler(enabled = selectedSubscription != null && selectedEpisode == null) {
        onSelectFeed(null)
    }

    if (selectedSubscription == null) {
        PodcastGridPage(
            modifier = modifier,
            state = state,
            onRefresh = onRefresh,
            onOpenSearch = onOpenSearch,
            onSelectFeed = onSelectFeed
        )
        return
    }

    if (selectedEpisode == null) {
        PodcastDetailPage(
            modifier = modifier,
            subscriptionTitle = selectedSubscription.title,
            subscriptionDescription = selectedSubscription.description,
            subscriptionImageUrl = selectedSubscription.imageUrl,
            episodes = episodesForSelectedPodcast,
            onRemoveFeed = { onRemoveFeed(selectedSubscription.id) },
            onSelectEpisode = { onSelectEpisode(it.id) }
        )
        return
    }

    EpisodeDetailPage(
        modifier = modifier,
        podcastTitle = selectedSubscription.title,
        episode = selectedEpisode,
        onPlayEpisode = { onPlayEpisode(selectedEpisode) },
        isCurrentEpisodeLoaded = playbackState.currentMediaId == "podcast:${selectedEpisode.id}",
        isCurrentEpisodePlaying = playbackState.currentMediaId == "podcast:${selectedEpisode.id}" &&
            playbackState.isPlaying
    )
}

@Composable
private fun PodcastGridPage(
    modifier: Modifier,
    state: PodcastUiState,
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit,
    onSelectFeed: (Long?) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Abos: ${state.subscriptions.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Aktualisieren"
                    )
                }
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Podcast suchen"
                    )
                }
            }
        }

        if (state.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (state.subscriptions.isEmpty() && !state.isLoading) {
            Text(
                text = "Noch keine Podcasts. Tippe auf die Lupe, um Podcasts zu suchen oder per RSS hinzuzufügen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.subscriptions, key = { it.id }) { subscription ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { onSelectFeed(subscription.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        if (subscription.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = subscription.imageUrl,
                                contentDescription = subscription.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = subscription.title,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastSearchPage(
    modifier: Modifier,
    state: PodcastUiState,
    onSearchQueryChange: (String) -> Unit,
    onSearchPodcasts: () -> Unit,
    onSubscribeFromSearch: (PodcastSearchResult) -> Unit,
    onFeedUrlChange: (String) -> Unit,
    onAddFeed: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Podcast suchen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Suchbegriff") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onSearchPodcasts) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                }
            }

            if (state.isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            state.searchErrorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = "Manuell per RSS hinzufügen",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.feedUrlInput,
                    onValueChange = onFeedUrlChange,
                    label = { Text("Podcast RSS URL") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onAddFeed) {
                    Text("Abo")
                }
            }

            if (state.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = "Suchergebnisse",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        items(state.searchResults, key = { it.id }) { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSubscribeFromSearch(result) },
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
                    if (result.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = result.imageUrl,
                            contentDescription = result.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(76.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(76.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(34.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (result.author.isNotBlank()) {
                            Text(
                                text = result.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (result.genre.isNotBlank()) {
                            Text(
                                text = result.genre,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastDetailPage(
    modifier: Modifier,
    subscriptionTitle: String,
    subscriptionDescription: String,
    subscriptionImageUrl: String,
    episodes: List<PodcastEpisode>,
    onRemoveFeed: () -> Unit,
    onSelectEpisode: (PodcastEpisode) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Text(
                text = subscriptionTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (subscriptionImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = subscriptionImageUrl,
                            contentDescription = subscriptionTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onRemoveFeed,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Abo löschen"
                        )
                    }
                }
            }

            DescriptionBlocksView(
                rawDescription = subscriptionDescription,
                textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = "Episoden",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        if (episodes.isEmpty()) {
            item {
                Text(
                    text = "Für diesen Podcast wurden keine Episoden gefunden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(episodes.take(200), key = { it.id }) { episode ->
                EpisodeListRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    episode = episode,
                    podcastFallbackImageUrl = subscriptionImageUrl,
                    onClick = { onSelectEpisode(episode) }
                )
            }
        }
    }
}

@Composable
private fun EpisodeDetailPage(
    modifier: Modifier,
    podcastTitle: String,
    episode: PodcastEpisode,
    onPlayEpisode: () -> Unit,
    isCurrentEpisodeLoaded: Boolean,
    isCurrentEpisodePlaying: Boolean
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Text(
                text = "Episode",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = podcastTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (episode.publishedAt > 0) {
                Text(
                    text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(episode.publishedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(
                onClick = onPlayEpisode,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Icon(
                    imageVector = if (isCurrentEpisodePlaying) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when {
                        isCurrentEpisodePlaying -> "Pausieren"
                        isCurrentEpisodeLoaded -> "Weiter abspielen"
                        else -> "Jetzt abspielen"
                    }
                )
            }

            DescriptionBlocksView(
                rawDescription = episode.description.ifBlank { "Keine Beschreibung verfügbar." },
                textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

private fun normalizeFeedUrl(url: String): String =
    url.trim().trimEnd('/').lowercase(Locale.US)

@Composable
private fun EpisodeListRow(
    modifier: Modifier = Modifier,
    episode: PodcastEpisode,
    podcastFallbackImageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            val imageUrl = episode.imageUrl.ifBlank { podcastFallbackImageUrl }
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatEpisodeMeta(episode.durationSec, episode.publishedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun formatEpisodeMeta(durationSec: Long?, publishedAt: Long): String {
    val durationText = durationSec?.let { "${formatDurationClock(it)} min" }
    val dateText = if (publishedAt > 0L) {
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(publishedAt))
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(publishedAt))
        "vom $date um $time Uhr"
    } else null

    return listOfNotNull(durationText, dateText).joinToString(", ").ifBlank { "Keine Metadaten" }
}

private fun formatDurationClock(totalSec: Long): String {
    val safe = totalSec.coerceAtLeast(0L)
    val h = safe / 3600L
    val m = (safe % 3600L) / 60L
    val s = safe % 60L
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}


private fun String.cleanDescription(): String =
    this
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</p>"), "\n\n")
        .replace(Regex("(?i)<p[^>]*>"), "")
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        // Keep chapter/time markers on separate lines.
        .replace(
            Regex("(?<=\\))\\s+(?=\\(\\d{1,2}:\\d{2}\\s*[-–]\\s*\\d{1,2}:\\d{2}\\))"),
            "\n"
        )
        .replace(Regex("[_]{3,}"), "\n\n")
        .replace(Regex("[-]{3,}"), "\n\n")
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lines()
        .map { it.trim() }
        .joinToString("\n")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

@Composable
private fun DescriptionBlocksView(
    rawDescription: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val blocks = rawDescription
        .cleanDescription()
        .parseDescriptionBlocks()

    if (blocks.isEmpty()) return

    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is DescriptionBlock.Paragraph -> {
                    Text(
                        text = block.text,
                        style = textStyle,
                        color = textColor
                    )
                }

                is DescriptionBlock.BulletList -> {
                    block.items.forEachIndexed { itemIndex, item ->
                        Text(
                            text = "- $item",
                            style = textStyle.copy(
                                textIndent = TextIndent(
                                    firstLine = 0.sp,
                                    restLine = 14.sp
                                )
                            ),
                            color = textColor
                        )
                        if (itemIndex != block.items.lastIndex) {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }

            if (index != blocks.lastIndex) {
                val next = blocks[index + 1]
                Spacer(
                    modifier = Modifier.height(
                        if (block is DescriptionBlock.Paragraph && next is DescriptionBlock.Paragraph) {
                            8.dp
                        } else {
                            4.dp
                        }
                    )
                )
            }
        }
    }
}

private fun String.parseDescriptionBlocks(): List<DescriptionBlock> {
    val bulletRegex = Regex("^[-•]\\s*")
    val timeLineRegex = Regex("^\\(\\d{1,2}:\\d{2}\\s*[-–]\\s*\\d{1,2}:\\d{2}\\)")
    val sourceLines = this
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lines()
        .map { it.trim() }

    fun isBulletLine(line: String): Boolean = bulletRegex.containsMatchIn(line)
    fun isTimelineLine(line: String): Boolean = timeLineRegex.containsMatchIn(line)

    val lines = mutableListOf<String>()
    for (line in sourceLines) {
        if (line.isBlank()) {
            if (lines.lastOrNull()?.isNotBlank() == true) {
                lines += ""
            }
        } else {
            lines += line
        }
    }
    while (lines.isNotEmpty() && lines.first().isBlank()) lines.removeAt(0)
    while (lines.isNotEmpty() && lines.last().isBlank()) lines.removeAt(lines.lastIndex)

    val blocks = mutableListOf<DescriptionBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (line.isBlank()) {
            i++
            continue
        }

        if (isTimelineLine(line)) {
            blocks += DescriptionBlock.Paragraph(line)
            i++
            continue
        }

        if (isBulletLine(line)) {
            val items = mutableListOf<String>()
            while (i < lines.size) {
                val bulletStart = lines[i]
                if (!isBulletLine(bulletStart)) break

                val bulletText = StringBuilder(
                    bulletStart.replaceFirst(bulletRegex, "").trim()
                )
                i++

                while (i < lines.size) {
                    val next = lines[i]
                    if (next.isBlank()) {
                        val upcoming = lines.getOrNull(i + 1)
                        if (upcoming != null && isBulletLine(upcoming)) {
                            i++ // compact blank between bullet points
                            break
                        }
                        i++ // paragraph break
                        break
                    }
                    if (isBulletLine(next)) break
                    if (bulletText.isNotEmpty()) bulletText.append(' ')
                    bulletText.append(next)
                    i++
                }

                items += bulletText.toString().trim()
            }
            if (items.isNotEmpty()) blocks += DescriptionBlock.BulletList(items)
            continue
        }

        val paragraph = StringBuilder(line)
        i++
        while (i < lines.size) {
            val next = lines[i]
            if (next.isBlank()) {
                i++
                break
            }
            if (isBulletLine(next) || isTimelineLine(next)) break
            paragraph.append(' ').append(next)
            i++
        }
        blocks += DescriptionBlock.Paragraph(paragraph.toString().trim())
    }

    return blocks
}

private sealed interface DescriptionBlock {
    data class Paragraph(val text: String) : DescriptionBlock
    data class BulletList(val items: List<String>) : DescriptionBlock
}
