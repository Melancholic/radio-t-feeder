package com.anagorny.radiot2telegram.services.impl

import com.anagorny.radiot2telegram.config.RssProperties
import com.anagorny.radiot2telegram.model.MetaInfoContainer
import com.anagorny.radiot2telegram.services.AbstractFeederService
import com.anagorny.radiot2telegram.services.FfmpegEncoder
import com.anagorny.radiot2telegram.services.TelegramBot
import org.slf4j.LoggerFactory
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.stereotype.Service
import java.util.concurrent.locks.ReentrantLock


@Service
class MainFeederService(
    threadPoolTaskExecutor: AsyncListenableTaskExecutor,
    ffmpegEncoder: FfmpegEncoder,
    feedFetcher: FeedFetcher,
    metaInfoContainer: MetaInfoContainer,
    hashTagsSuggestionService: HashTagsSuggestionService,
    telegramBot: TelegramBot,
    private val rssProperties: RssProperties
) : AbstractFeederService(
    threadPoolTaskExecutor,
    ffmpegEncoder,
    feedFetcher,
    metaInfoContainer,
    hashTagsSuggestionService,
    telegramBot
) {
    override val logger = LoggerFactory.getLogger(MainFeederService::class.java)
    private val locker = ReentrantLock()

    fun readRssFeed() {
        if (locker.isLocked) throw java.lang.RuntimeException("Archive already processing")
        locker.lock()
        val startDate = System.currentTimeMillis()
        try {
            logger.info("Main RSS read started!")
            val newEntries = getMostRecentNews(rssProperties.main.url)

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
                doProcessingFeed(future.get())
            }
        } finally {
            locker.unlock()
            logger.info("Archive RSS has been processed on ${System.currentTimeMillis() - startDate} ms")
        }
    }
}

