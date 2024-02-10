package com.anagorny.radiot2telegram.services.impl

import com.anagorny.radiot2telegram.config.RssProperties
import com.anagorny.radiot2telegram.model.FeedItemWithFile
import com.anagorny.radiot2telegram.model.MetaInfoContainer
import com.anagorny.radiot2telegram.services.AbstractFeederService
import com.anagorny.radiot2telegram.services.FfmpegEncoder
import com.anagorny.radiot2telegram.services.TelegramBot
import com.rometools.rome.feed.synd.SyndEntry
import mu.KLogging
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantLock


@Service
class ArchiveFeederService(
    threadPoolTaskExecutor: AsyncTaskExecutor,
    ffmpegEncoder: FfmpegEncoder,
    feedFetcher: FeedFetcher,
    metaInfoContainer: MetaInfoContainer,
    hashTagsSuggestionService: HashTagsSuggestionService,
    telegramBot: TelegramBot,
    val rssProperties: RssProperties
) : AbstractFeederService(
    threadPoolTaskExecutor,
    ffmpegEncoder,
    feedFetcher,
    metaInfoContainer,
    hashTagsSuggestionService,
    telegramBot
) {
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
                .chunked(rssProperties.archive.batchSize.coerceAtLeast(1))
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
                    doProcessingFeed(future.get())
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
        return getMostRecentNews(rssProperties.archive.url)
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

    private companion object : KLogging()
}

