package com.anagorny.radiot2telegram.service

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
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock


@Service
class MainFeederService {
    private val logger = LoggerFactory.getLogger(MainFeederService::class.java)

    @Autowired
    lateinit var ffmpegEncoder: FfmpegEncoder

    @Autowired
    lateinit var telegramBot: TelegramBot

    @Autowired
    lateinit var threadPoolTaskExecutor: AsyncListenableTaskExecutor

    @Autowired
    lateinit var metaInfoContainer: MetaInfoContainer

    @Value("\${radio_t_feeder.main.rss.url}")
    private lateinit var mainRssUrl: String

    private val offset: AtomicInteger = AtomicInteger()

    private var startOffset: Int = 0

    private val locker = ReentrantLock()

    @Autowired
    private lateinit var feedFetcher: FeedFetcher

    fun readRssFeed() {
        if (locker.isLocked) throw java.lang.RuntimeException("Archive already processing")
        locker.lock()
        val startDate = System.currentTimeMillis()
        try {
            logger.info("Main RSS read started!")
            val newEntries = getMostRecentNews(mainRssUrl)

            val total = newEntries.size

            if (total == 0) {
                logger.info("RSS  feed received $total new entries, nothing to do...")
                return
            } else {
                logger.info("RSS  feed received $total new entries, start processing...")
            }
            val futuresWithDownloaded = newEntries.withIndex()
                    .map { (index, value) -> asyncPreparingFile(value, total, index + 1) }

            for (future in futuresWithDownloaded) {
                val feedWithFile = future.get()
                val feed = feedWithFile.item
                val file = feedWithFile.file
                if (file != null) {
                    try {
                        val message = telegramBot.sendAudio(file, feed)
                        if (message == null) {
                            logger.error("Message '${feed.title}' cant sended to Telegram (message is null!)")
                            metaInfoContainer.appendToEnd(feed)
                        } else {
                            logger.info("Message '${feed.title}' successfully sended to Telegram with offset = $offset")
                            feed.tgMessageId = message.messageId
                            feed.tgFileId = message.audio.fileId
                            metaInfoContainer.appendToEnd(feed)
                        }
                        logger.info("Message with '${feed.title}' successfully processed")
                    } catch (e: Exception) {
                        throw e
                    } finally {
                        removeFile(file, logger)
                    }
                }
            }
        } finally {
            locker.unlock()
            logger.info("Archive RSS has been processed on ${System.currentTimeMillis() - startDate} ms")

        }

    }

    //TODO  move to common
    private fun asyncPreparingFile(entry: SyndEntry, total: Int, current: Int): Future<FeedItemWithFile> = threadPoolTaskExecutor.submit(Callable<FeedItemWithFile> {
        val startDate = System.currentTimeMillis()
        val feed = buildFeedItem(entry)
        logger.info("Feed '${feed.title}' is builded, downloading...")
        val file = ffmpegEncoder.downloadAndCompressMp3(feed)
        logger.info("$current/$total files downloaded and compressed on ${System.currentTimeMillis() - startDate} ms")
        return@Callable FeedItemWithFile(item = feed, file = file)
    })


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

