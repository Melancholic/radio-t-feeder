package com.anagorny.radiot2telegram.service

import com.anagorny.radiot2telegram.model.FeedItemWithFile
import com.rometools.rome.feed.synd.SyndEntry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantLock


@Service
class ArchiveFeederService : AbstractFeederService() {
    override val logger = LoggerFactory.getLogger(ArchiveFeederService::class.java)

    @Autowired
    lateinit var telegramBot: TelegramBot

    @Value("\${radio_t_feeder.archive.rss.url}")
    private lateinit var archiveRssUrl: String

    @Value("\${radio_t_feeder.archive.batch_size}")
    private var batchSize: Int = 5

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

            val groupedEntries = entries.asSequence()
                    .mapIndexed { index, value -> (index to value) }
                    .chunked(Math.max(batchSize, 1))
                    .toList()

            for ((groupNum, group) in groupedEntries.withIndex()) {
                val futuresWithDownloaded = ConcurrentHashMap<Int, Future<FeedItemWithFile>>()

                val groupStartDate = System.currentTimeMillis()
                for ((index, entry) in group) {
                    val future: Future<FeedItemWithFile> = asyncPreparingFile(entry, total, index + 1)
                    futuresWithDownloaded.putIfAbsent(index, future)
                }
                for (offset in futuresWithDownloaded.keys().asSequence().sorted()) {
                    val future = futuresWithDownloaded.getValue(offset)
                    val feedWithFile = future.get()
                    val feed = feedWithFile.item

                    lateinit var file: File
                    try {
                        file = File(feedWithFile.filePath)
                    } catch (e: Exception) {
                        logger.error("Cant read file '${feedWithFile.filePath}' fo sending", e)
                        throw e
                    }

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
                logger.info("Group ${groupNum + 1}/${groupedEntries.size} has been processed on ${(System.currentTimeMillis() - groupStartDate) / 1000 / 60.0} minutes")
            }

            logger.info("Archive RSS has been processed on ${(System.currentTimeMillis() - startDate) / 1000 / 60.0} minutes")
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

    fun archiveIsSynced(): Boolean {
        if (locker.isLocked) return false
        locker.lock()
        try {
            return getNotSyncedEntries().isEmpty()
        } finally {
            locker.unlock()
        }
    }

}

