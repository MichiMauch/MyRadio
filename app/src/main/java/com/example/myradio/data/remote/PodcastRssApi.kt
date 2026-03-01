package com.example.myradio.data.remote

import android.util.Log
import com.example.myradio.data.model.PodcastEpisode
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.GZIPInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

data class PodcastFeedResult(
    val feedUrl: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val episodes: List<PodcastEpisode>
)

data class PodcastFetchResult(
    val feed: PodcastFeedResult?,
    val errorMessage: String?
)

object PodcastRssApi {

    private const val TAG = "PodcastRssApi"
    private const val MAX_REDIRECTS = 5

    fun fetchFeed(feedUrl: String, maxEpisodes: Int = 50): PodcastFeedResult? {
        return fetchFeedDetailed(feedUrl, maxEpisodes).feed
    }

    fun fetchFeedDetailed(feedUrl: String, maxEpisodes: Int = 50): PodcastFetchResult {
        if (feedUrl.isBlank()) {
            return PodcastFetchResult(feed = null, errorMessage = "Leere Feed-URL")
        }

        val normalized = feedUrl.trim()
        return try {
            val response = fetchBytesWithRedirects(normalized)
            if (response.code !in 200..299) {
                val message = "HTTP ${response.code}${response.message?.let { " ($it)" } ?: ""}"
                Log.e(TAG, "Feed request failed: $message for $normalized")
                return PodcastFetchResult(feed = null, errorMessage = message)
            }

            val parsed = try {
                parseFeed(response.bytes, normalized, maxEpisodes)
            } catch (e: Exception) {
                Log.e(TAG, "XML parsing failed for $normalized", e)
                null
            }

            if (parsed == null) {
                PodcastFetchResult(feed = null, errorMessage = "XML konnte nicht gelesen werden")
            } else {
                PodcastFetchResult(feed = parsed, errorMessage = null)
            }
        } catch (e: javax.net.ssl.SSLException) {
            Log.e(TAG, "SSL error for $normalized", e)
            PodcastFetchResult(feed = null, errorMessage = "SSL-Fehler: ${e.javaClass.simpleName}")
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Unknown host for $normalized", e)
            PodcastFetchResult(feed = null, errorMessage = "Host nicht erreichbar")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Timeout for $normalized", e)
            PodcastFetchResult(feed = null, errorMessage = "Zeitüberschreitung beim Laden")
        } catch (e: Exception) {
            Log.e(TAG, "fetchFeed failed for $normalized", e)
            PodcastFetchResult(feed = null, errorMessage = e.javaClass.simpleName)
        }
    }

    private data class HttpBytesResponse(
        val code: Int,
        val message: String?,
        val bytes: ByteArray
    )

    private fun fetchBytesWithRedirects(url: String): HttpBytesResponse {
        var currentUrl = url
        var redirects = 0

        while (true) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
                setRequestProperty("Accept-Encoding", "gzip")
                setRequestProperty("Accept-Language", "de-CH,de;q=0.9,en;q=0.8")
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"
                )
            }

            val code = connection.responseCode
            val message = connection.responseMessage

            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank()) {
                    return HttpBytesResponse(code = code, message = "Redirect ohne Location", bytes = ByteArray(0))
                }
                if (redirects >= MAX_REDIRECTS) {
                    return HttpBytesResponse(code = code, message = "Zu viele Redirects", bytes = ByteArray(0))
                }
                currentUrl = URL(URL(currentUrl), location).toString()
                redirects += 1
                continue
            }

            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val bytes = stream?.use { input ->
                val encoding = connection.contentEncoding.orEmpty().lowercase(Locale.US)
                val buffered = BufferedInputStream(input)
                if (encoding.contains("gzip")) {
                    GZIPInputStream(buffered).readBytes()
                } else {
                    buffered.readBytes()
                }
            } ?: ByteArray(0)

            connection.disconnect()
            return HttpBytesResponse(code = code, message = message, bytes = bytes)
        }
    }

    private fun parseFeed(xmlBytes: ByteArray, feedUrl: String, maxEpisodes: Int): PodcastFeedResult {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        } catch (_: Exception) {
            // best effort on older parsers
        }

        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
        val root = document.documentElement

        val channel = firstDescendant(root, "channel")
        val feed = if (localName(root) == "feed") root else null
        val metaParent = channel ?: feed

        val channelTitle = metaParent?.let { firstChildText(it, "title") }.orEmpty().ifBlank { feedUrl }
        val channelDescription = metaParent?.let { firstChildText(it, "description", "subtitle") }.orEmpty()
        val channelImageUrl = resolveFeedImage(metaParent)

        val itemNodes = if (channel != null) descendantsByName(channel, "item") else descendantsByName(root, "entry")
        val episodes = mutableListOf<PodcastEpisode>()

        for (item in itemNodes) {
            val title = firstChildText(item, "title").orEmpty()
            if (title.isBlank()) continue

            val description = firstChildText(item, "description", "summary", "content").orEmpty()
            val publishedAt = parsePubDate(firstChildText(item, "pubDate", "published", "updated", "date").orEmpty())
            val audioUrl = resolveAudioUrl(item)
            val episodeImageUrl = resolveEpisodeImage(item, channelImageUrl)
            val durationSec = parseDurationToSeconds(firstChildText(item, "duration").orEmpty())
            if (audioUrl.isBlank()) continue

            episodes += PodcastEpisode(
                id = "$feedUrl|$title|$audioUrl",
                feedUrl = feedUrl,
                podcastTitle = channelTitle,
                title = title,
                audioUrl = audioUrl,
                description = description,
                publishedAt = publishedAt,
                imageUrl = episodeImageUrl,
                durationSec = durationSec
            )

            if (episodes.size >= maxEpisodes) break
        }

        return PodcastFeedResult(
            feedUrl = feedUrl,
            title = channelTitle,
            description = channelDescription,
            imageUrl = channelImageUrl,
            episodes = episodes.sortedByDescending { it.publishedAt }
        )
    }

    private fun parsePubDate(value: String): Long {
        if (value.isBlank()) return 0L
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm Z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val formatter = SimpleDateFormat(pattern, Locale.US)
                formatter.isLenient = true
                val date = formatter.parse(value)
                if (date != null) return date.time
            } catch (_: Exception) {
                // try next
            }
        }
        return 0L
    }

    private fun resolveFeedImage(metaParent: Element?): String {
        if (metaParent == null) return ""

        val itunesImage = descendantsByName(metaParent, "image")
            .firstOrNull { rawName(it).contains("itunes:", ignoreCase = true) }
            ?.let { attrAny(it, "href", "url") }
            .orEmpty()
        if (itunesImage.isNotBlank()) return itunesImage

        val atomImage = descendantsByName(metaParent, "image")
            .firstOrNull()
            ?.let { attrAny(it, "href", "url") }
            .orEmpty()
        if (atomImage.isNotBlank()) return atomImage

        val rssImage = firstDescendant(metaParent, "image")
            ?.let { firstChildText(it, "url") }
            .orEmpty()
        if (rssImage.isNotBlank()) return rssImage

        return ""
    }

    private fun resolveAudioUrl(item: Element): String {
        descendantsByName(item, "enclosure").forEach { enclosure ->
            val url = attrAny(enclosure, "url", "href")
            val type = attrAny(enclosure, "type")
            if (url.isNotBlank() && (type.isBlank() || type.startsWith("audio/", ignoreCase = true))) {
                return url
            }
        }

        descendantsByName(item, "link").forEach { link ->
            val rel = attrAny(link, "rel")
            val href = attrAny(link, "href", "url")
            val type = attrAny(link, "type")
            val text = link.textContent.orEmpty().trim()
            if (href.isNotBlank()) {
                if (rel.equals("enclosure", ignoreCase = true) ||
                    type.startsWith("audio/", ignoreCase = true) ||
                    looksLikeAudioUrl(href)
                ) {
                    return href
                }
            }
            if (text.isNotBlank() && looksLikeAudioUrl(text)) {
                return text
            }
        }

        val explicitLink = firstChildText(item, "guid", "link").orEmpty()
        if (looksLikeAudioUrl(explicitLink)) return explicitLink
        return ""
    }

    private fun resolveEpisodeImage(item: Element, fallbackFeedImage: String): String {
        val itunesImage = descendantsByName(item, "image")
            .firstOrNull { rawName(it).contains("itunes:", ignoreCase = true) }
            ?.let { attrAny(it, "href", "url") }
            .orEmpty()
        if (itunesImage.isNotBlank()) return itunesImage

        val mediaThumbnail = descendantsByName(item, "thumbnail")
            .firstOrNull()
            ?.let { attrAny(it, "url", "href") }
            .orEmpty()
        if (mediaThumbnail.isNotBlank()) return mediaThumbnail

        val mediaImage = descendantsByName(item, "content")
            .firstOrNull {
                val type = attrAny(it, "type")
                type.startsWith("image/", ignoreCase = true)
            }
            ?.let { attrAny(it, "url", "href") }
            .orEmpty()
        if (mediaImage.isNotBlank()) return mediaImage

        return fallbackFeedImage
    }

    private fun parseDurationToSeconds(raw: String): Long? {
        val value = raw.trim()
        if (value.isBlank()) return null
        if (value.all { it.isDigit() }) return value.toLongOrNull()

        val parts = value.split(":").map { it.trim() }
        return when (parts.size) {
            2 -> {
                val m = parts[0].toLongOrNull() ?: return null
                val s = parts[1].toLongOrNull() ?: return null
                m * 60L + s
            }
            3 -> {
                val h = parts[0].toLongOrNull() ?: return null
                val m = parts[1].toLongOrNull() ?: return null
                val s = parts[2].toLongOrNull() ?: return null
                h * 3600L + m * 60L + s
            }
            else -> null
        }
    }

    private fun looksLikeAudioUrl(url: String): Boolean {
        val lower = url.lowercase(Locale.US)
        return lower.endsWith(".mp3") ||
            lower.endsWith(".m4a") ||
            lower.endsWith(".aac") ||
            lower.endsWith(".ogg") ||
            lower.contains(".mp3?") ||
            lower.contains(".m4a?")
    }

    private fun descendantsByName(parent: Element, local: String): List<Element> {
        val nodes = parent.getElementsByTagName("*")
        val result = mutableListOf<Element>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node is Element && localName(node).equals(local, ignoreCase = true)) {
                result += node
            }
        }
        return result
    }

    private fun firstDescendant(parent: Element, local: String): Element? =
        descendantsByName(parent, local).firstOrNull()

    private fun firstChildText(parent: Element, vararg names: String): String? {
        val wanted = names.map { it.lowercase(Locale.US) }.toSet()
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element && localName(node).lowercase(Locale.US) in wanted) {
                val text = node.textContent.orEmpty().trim()
                if (text.isNotBlank()) return text
            }
        }
        return null
    }

    private fun localName(node: Node): String {
        val local = node.localName
        if (!local.isNullOrBlank()) return local
        val raw = node.nodeName.orEmpty()
        return raw.substringAfter(':', raw)
    }

    private fun rawName(node: Node): String = node.nodeName.orEmpty()

    private fun attrAny(element: Element, vararg names: String): String {
        names.forEach { name ->
            val direct = element.getAttribute(name).orEmpty().trim()
            if (direct.isNotBlank()) return direct
        }
        val attributes = element.attributes ?: return ""
        for (i in 0 until attributes.length) {
            val attr = attributes.item(i)
            val attrName = localName(attr).lowercase(Locale.US)
            if (names.any { it.equals(attrName, ignoreCase = true) }) {
                val value = attr.nodeValue.orEmpty().trim()
                if (value.isNotBlank()) return value
            }
        }
        return ""
    }
}
