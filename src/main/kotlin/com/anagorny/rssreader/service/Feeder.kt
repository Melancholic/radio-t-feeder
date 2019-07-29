package com.anagorny.rssreader.service

import com.rometools.fetcher.FetcherException
import com.rometools.fetcher.impl.HttpURLFeedFetcher
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.FeedException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.IOException
import java.net.URL


@Service
class Feeder {
    private val logger = LoggerFactory.getLogger(Feeder::class.java)

    @Autowired
    lateinit var ffmpegEncoder: FfmpegEncoder

    @Autowired
    lateinit var telegramBot: TelegramBot

    @Scheduled(fixedDelay = 10_000)
    fun refresh() {
        val startDate = System.currentTimeMillis()
        logger.info("Rss read started!")
        val entries = getMostRecentNews("https://radio-t.com/podcast-archives.rss")//.reversed()
        logger.info("RSS  feed received with count = ${entries.size}")
        for (entry in entries) {
            val feed = FeedItem(
                    title = entry.title,
                    authors = entry.authors.joinToString { ", " },
                    audioUrl = entry.enclosures.firstOrNull()?.url ?: "",
                    audioType = entry.enclosures.firstOrNull()?.type ?: "",
                    description = rawDescription(entry.description?.value ?: "", entry.uri),
                    descriptionType = entry.description?.type ?: "",
                    podcastUrl = entry.uri,
                    publishedDate = entry.publishedDate,
                    thumbUrl = entry.foreignMarkup?.first{ x -> x.name == "image" }?.attributes?.first()?.value
            )
            val file = ffmpegEncoder.downloadAndCompressMp3(feed.audioUrl)
            try {
                //TODO processing message
                    val message = telegramBot.sendAudio(file, feed) ?: throw TelegramApiException("Message is null")
                removeFile(file, logger)
            } catch (e: Exception) {
                logger.error("Message cant send to Telegram", e)
            }
        }
        logger.info("DONE! ${System.currentTimeMillis() - startDate} ms")

    }

    fun getMostRecentNews(feedUrl: String): List<SyndEntry> {
        try {
            return retrieveFeed(feedUrl).entries ?: emptyList()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    @Throws(IOException::class, FeedException::class, FetcherException::class)
    private fun retrieveFeed(feedUrl: String): SyndFeed {
        val feedFetcher = HttpURLFeedFetcher()
        return feedFetcher.retrieveFeed(URL(feedUrl))
    }
}
