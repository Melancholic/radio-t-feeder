package com.anagorny.radiot2telegram.service

import com.anagorny.radiot2telegram.helpers.removeFile
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.locks.ReentrantLock


@Service
class MainFeederService : AbstractFeederService() {
    override val logger = LoggerFactory.getLogger(MainFeederService::class.java)

    @Autowired
    lateinit var telegramBot: TelegramBot

    @Value("\${radio_t_feeder.main.rss.url}")
    private lateinit var mainRssUrl: String

    private val locker = ReentrantLock()


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
                val (feed, filePath, _) = feedWithFile

                try {
                    val message = telegramBot.sendAudio(feedWithFile)
                    if (message == null) {
                        logger.error("Message '${feed.title}' cant sended to Telegram (message is null!)")
                        metaInfoContainer.appendToEnd(feed)
                    } else {
                        logger.info("Message '${feed.title}' successfully sended to Telegram")
                        feed.tgMessageId = message.messageId
                        feed.tgFileId = message.audio.fileId
                        metaInfoContainer.appendToEnd(feed)
                    }
                    logger.info("Message with '${feed.title}' successfully processed")
                } catch (e: Exception) {
                    throw e
                } finally {
                    removeFile(filePath, logger)
                }
            }
        } finally {
            locker.unlock()
            logger.info("Archive RSS has been processed on ${System.currentTimeMillis() - startDate} ms")

        }

    }
}

