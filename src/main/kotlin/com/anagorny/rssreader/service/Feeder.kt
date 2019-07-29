package com.anagorny.rssreader.service

import com.rometools.fetcher.FetcherException
import com.rometools.fetcher.impl.HttpURLFeedFetcher
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.FeedException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.IOException
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Future
import javax.annotation.PostConstruct


@Service
class Feeder {
    private val logger = LoggerFactory.getLogger(Feeder::class.java)

    @Autowired
    lateinit var ffmpegEncoder: FfmpegEncoder

    @Autowired
    lateinit var telegramBot: TelegramBot

    @Autowired
    lateinit var threadPoolTaskExecutor: AsyncListenableTaskExecutor

    @PostConstruct
    fun refresh() {
        val startDate = System.currentTimeMillis()
        logger.info("Rss read started!")
        val entries = getMostRecentNews("https://radio-t.com/podcast-archives.rss").reversed()
        logger.info("RSS  feed received with count = ${entries.size}")
        val  futuresWithDownloaded = LinkedHashSet<Future<FeedItemWithFile>>()
        var filesDoneCount: Int = 0
        for (entry in entries) {

            val future : Future<FeedItemWithFile> = threadPoolTaskExecutor.submit(Callable<FeedItemWithFile> {
                val feed = buildFeedItem(entry)
                logger.info("Feed '${feed.title}' is builded, downloading...")
                val file = ffmpegEncoder.downloadAndCompressMp3(feed.audioUrl)
                logger.info("${++filesDoneCount}/${entries.size} files downloaded and compressed on ${System.currentTimeMillis() - startDate} ms")
                return@Callable FeedItemWithFile(item = feed, file = file)
            })

            futuresWithDownloaded.add(future)
        }

        for (future in futuresWithDownloaded) {
            try {
                val feedWithFile = future.get()
                val feed = feedWithFile.item
                val file = feedWithFile.file
                if (file != null) {
                    try {
                        //TODO processing message
                        val message = telegramBot.sendAudio(file, feed) ?: throw TelegramApiException("Message is null")
                        removeFile(file, logger)
                    } catch (e: Exception) {
                        logger.error("Message cant send to Telegram", e)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error while process: ", e)
            }
        }

        logger.info("DONE! ${System.currentTimeMillis() - startDate} ms")

    }

    fun buildFeedItem(entry: SyndEntry) = FeedItem(
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