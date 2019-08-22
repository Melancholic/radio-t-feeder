package com.anagorny.radiot2telegram.service

import com.anagorny.radiot2telegram.model.FeedItem
import com.anagorny.radiot2telegram.model.FeedItemWithFile
import com.anagorny.radiot2telegram.model.MetaInfoContainer
import com.rometools.rome.feed.synd.SyndEntry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantLock


@Service
class ArchiveFeederService {
    private val logger = LoggerFactory.getLogger(ArchiveFeederService::class.java)

    @Autowired
    lateinit var ffmpegEncoder: FfmpegEncoder

    @Autowired
    lateinit var telegramBot: TelegramBot

    @Autowired
    lateinit var threadPoolTaskExecutor: AsyncListenableTaskExecutor

    @Autowired
    lateinit var metaInfoContainer: MetaInfoContainer

    @Autowired
    private lateinit var feedFetcher: FeedFetcher

    @Value("\${radio_t_feeder.archive.rss.url}")
    private lateinit var archiveRssUrl: String

    private val locker = ReentrantLock()

    fun archiveProcessing() {
        if (locker.isLocked) throw java.lang.RuntimeException("Archive already processing")
        locker.lock()
        try {
            val startDate = System.currentTimeMillis()
            logger.info("Archive RSS read started!")
            val entries = getNotSyncedEntries()
            val total = entries.size
            logger.info("RSS feed received with $total new entries")
            val futuresWithDownloaded = ConcurrentHashMap<Int, Future<FeedItemWithFile>>()

            for ((index, entry) in entries.withIndex()) {
                val future: Future<FeedItemWithFile> = asyncPreparingFile(entry, total, index + 1)
                futuresWithDownloaded.putIfAbsent(index, future)
            }


            for (offset in futuresWithDownloaded.keys().asSequence().sorted()) {
                val future = futuresWithDownloaded.getValue(offset)
                val feedWithFile = future.get()
                val feed = feedWithFile.item
                val file = feedWithFile.file
                if (file != null) {
                    try {
                        val message = telegramBot.sendAudio(file, feed)
                        if (message == null) {
                            logger.error("Message '${feed.title}' cant sended to Telegram (response is null!)")
                            metaInfoContainer.appendToEnd(feed)
                        } else {
                            logger.info("Message '${feed.title}' successfully sended to Telegram")
                            feed.tgMessageId = message.messageId
                            feed.tgFileId = message.audio.fileId
                            metaInfoContainer.appendToEnd(feed)
                        }
                        logger.info("Message '${feed.title}' successfully processed")
                    } catch (e: Exception) {
                        throw e
                    } finally {
                        removeFile(file, logger)
                    }
                }
            }
            logger.info("Archive RSS has been processed on ${System.currentTimeMillis() - startDate} ms")
        } finally {
            locker.unlock()
        }

    }

    private fun getNotSyncedEntries(): Set<SyndEntry> {
        val latestPublishDate = metaInfoContainer.metaInfoEntity.lastPublishedTime ?: Date(0)
        return getMostRecentNews(archiveRssUrl)
                .filter { it.publishedDate > latestPublishDate }
                .toSet()
    }

    //TODO error
    fun archiveIsSynced(): Boolean {
        if (locker.isLocked) return false
        locker.lock()
        try {
            return getNotSyncedEntries().isEmpty()
        } finally {
            locker.unlock()
        }
    }

    private fun asyncPreparingFile(entry: SyndEntry, total: Int, current: Int): Future<FeedItemWithFile> = threadPoolTaskExecutor.submit(Callable<FeedItemWithFile> {
        val startDate = System.currentTimeMillis()
        val feed = buildFeedItem(entry)
        logger.info("Feed '${feed.title}' is builded, downloading...")
        val file = ffmpegEncoder.downloadAndCompressMp3(feed)
        logger.info("$current/$total files downloaded and compressed on ${(System.currentTimeMillis() - startDate) / 1000} sec")
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

    private fun getMostRecentNews(feedUrl: String): List<SyndEntry> {
        try {
            return feedFetcher.downloadFeed(feedUrl).entries
                    .asSequence()
                    .sortedBy { it.publishedDate } // Сортируем по дате публикации
                    .filter {
                        it.publishedDate > (metaInfoContainer.metaInfoEntity.lastPublishedTime ?: Date(0))
                    } // Отфильтровываем уже обработанные публикации
                    .toList()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}

