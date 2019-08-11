package com.anagorny.rssreader.service

import com.anagorny.rssreader.model.FeedItem
import com.anagorny.rssreader.model.FeedItemWithFile
import com.anagorny.rssreader.model.MetaInfoContainer
import com.rometools.fetcher.FetcherException
import com.rometools.fetcher.impl.HttpURLFeedFetcher
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.FeedException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
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

    @Autowired
    lateinit var metaInfoContainer: MetaInfoContainer

    private val offset: AtomicInteger = AtomicInteger()

    private var startOffset: Int = 0

    @PostConstruct
    private fun init() {
        startOffset = metaInfoContainer.metaInfoEntity.lastOffset ?: 0
        offset.set(startOffset)
    }

    fun archiveProcessing() {
        val startDate = System.currentTimeMillis()
        logger.info("Archive RSS read started!")
        val entries = getMostRecentNews("https://radio-t.com/podcast-archives.rss").reversed()
        val total = entries.size - startOffset
        logger.info("RSS  feed received with count = $total")
        val futuresWithDownloaded = ConcurrentHashMap<Int, Future<FeedItemWithFile>>()

        for ((index, entry) in entries.withIndex().filter { it.index > startOffset }) {
            val future: Future<FeedItemWithFile> = asyncPreparingFile(entry, total, index + 1)
            futuresWithDownloaded.putIfAbsent(index, future)
        }


        for (offset in futuresWithDownloaded.keys().asSequence().sorted()) {
            val future = futuresWithDownloaded.getValue(offset)
            val feedWithFile = future.get()
            val feed = feedWithFile.item
            val file = feedWithFile.file
            if (file != null) {
                //TODO processing message
                try {
                    val message = telegramBot.sendAudio(file, feed)
                    if (message == null) {
                        logger.error("Message with offset=$offset cant sended to Telegram (message is null!!!)")
                        metaInfoContainer.append(offset, feed)
                    } else {
                        logger.info("Message with offset=$offset successfully sended to Telegram")
                        feed.tgMessageId = message.messageId
                        feed.tgFileId = message.audio.fileId
                        metaInfoContainer.append(offset, feed)
                    }
                    logger.info("Message with offset=$offset successfully processed")
                } catch (e: Exception) {
                    throw e
                } finally {
                    removeFile(file, logger)
                }
            }
        }
        logger.info("Archive RSS has been processed on ${System.currentTimeMillis() - startDate} ms")

    }

    private fun asyncPreparingFile(entry: SyndEntry, total: Int, current: Int): Future<FeedItemWithFile> = threadPoolTaskExecutor.submit(Callable<FeedItemWithFile> {
        val startDate = System.currentTimeMillis()
        val feed = buildFeedItem(entry)
        logger.info("Feed '${feed.title}' is builded, downloading...")
        val file = ffmpegEncoder.downloadAndCompressMp3(feed)
        logger.info("$current/$total files downloaded and compressed on ${System.currentTimeMillis() - startDate} ms")
        return@Callable FeedItemWithFile(item = feed, file = file)
    })


    private fun buildFeedItem(entry: SyndEntry): FeedItem {
        val description = parseDescription(entry.description?.value ?: "", entry.uri)
        val audioUrlAlter = entry.enclosures.firstOrNull()?.url
        val audioUrl = parseAudioUrl(entry.description?.value ?: "") ?: audioUrlAlter
        return FeedItem(
                title = entry.title,
                authors = entry.authors.joinToString { ", " },
                audioType = entry.enclosures.firstOrNull()?.type ?: "",
                audioUrlAlter = audioUrlAlter,
                audioUrl = audioUrl,
                description = description,
                descriptionType = entry.description?.type ?: "",
                podcastUrl = entry.uri,
                publishedDate = entry.publishedDate,
                thumbUrl = entry.foreignMarkup?.first { x -> x.name == "image" }?.attributes?.first()?.value
        )
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

