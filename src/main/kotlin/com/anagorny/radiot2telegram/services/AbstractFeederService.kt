package com.anagorny.radiot2telegram.services

import com.anagorny.radiot2telegram.helpers.parseAudioUrl
import com.anagorny.radiot2telegram.helpers.parseDescription
import com.anagorny.radiot2telegram.helpers.removeFile
import com.anagorny.radiot2telegram.helpers.removeFileIfExist
import com.anagorny.radiot2telegram.model.FeedItem
import com.anagorny.radiot2telegram.model.FeedItemWithFile
import com.anagorny.radiot2telegram.model.MetaInfoContainer
import com.anagorny.radiot2telegram.services.impl.FeedFetcher
import com.anagorny.radiot2telegram.services.impl.HashTagsSuggestionService
import com.rometools.rome.feed.synd.SyndEntry
import mu.KLogging
import org.springframework.core.task.AsyncTaskExecutor
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Future

abstract class AbstractFeederService(
    private val threadPoolTaskExecutor: AsyncTaskExecutor,
    private val ffmpegEncoder: FfmpegEncoder,
    private val feedFetcher: FeedFetcher,
    protected val metaInfoContainer: MetaInfoContainer,
    private val hashTagsSuggestionService: HashTagsSuggestionService,
    private val telegramBot: TelegramBot
) {
    protected fun doProcessingFeed(feedWithFile: FeedItemWithFile) {
        val (feed, audioFilePath, _, thumbFilePath) = feedWithFile

        try {
            val message = telegramBot.sendAudio(feedWithFile)
            if (message == null) {
                logger.error("Message '${feed.title}' cant sent to Telegram (message is null!)")
                metaInfoContainer.appendToEnd(feed)
            } else {
                logger.info("Message '${feed.title}' successfully sent to Telegram")
                feed.tgMessageId = message.messageId
                feed.tgFileId = message.audio.fileId
                metaInfoContainer.appendToEnd(feed)
            }
            logger.info("Message '${feed.title}' successfully processed")
        } catch (e: Exception) {
            throw e
        } finally {
            removeFile(audioFilePath, logger)
            removeFileIfExist(thumbFilePath, logger)
        }
    }

    protected fun getMostRecentNews(feedUrl: String): List<SyndEntry> {
        try {
            return feedFetcher.downloadFeed(feedUrl).entries.asSequence()
                .sortedBy { it.publishedDate } // Сортируем по дате публикации
                .filter {
                    it.publishedDate > (metaInfoContainer.metaInfoEntity.lastPublishedTime ?: Date(0))
                } // Отфильтровываем уже обработанные публикации
                .toList()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun buildFeedItem(entry: SyndEntry): FeedItem {
        var descriptionSB = StringBuilder(
            parseDescription(
                entry.description?.value ?: "", entry.uri
            )
        )
        val audioUrlAlter = entry.enclosures.firstOrNull()?.url
        val audioUrl = parseAudioUrl(entry.description?.value ?: "") ?: audioUrlAlter
        ?: throw RuntimeException("Cannot found any link to audio file with podcast (all audio urls is null)")


        val (hashtags, hashtagsStr) = hashTagsSuggestionService.getHashtagsFromDescription(
            entry.title, entry.description.value ?: ""
        )

        if (hashtagsStr.isNotEmpty()) {
            descriptionSB.append("\n").append("\n").append(hashtagsStr)
        }

        return FeedItem(
            title = entry.title,
            authors = entry.authors.map { it.name }.joinToString { ", " }.ifEmpty { DEFAULT_AUTHORS },
            audioType = entry.enclosures.firstOrNull()?.type ?: "",
            audioUrlAlter = audioUrlAlter,
            audioUrl = audioUrl,
            description = descriptionSB.toString(),
            descriptionType = entry.description?.type ?: "",
            podcastUrl = entry.uri,
            publishedDate = entry.publishedDate,
            thumbUrl = entry.foreignMarkup?.first { x -> x.name == "image" }?.attributes?.first()?.value,
            hashtags = hashtags
        )
    }


    protected fun asyncPreparingFile(entry: SyndEntry, total: Int, current: Int): Future<FeedItemWithFile> =
        threadPoolTaskExecutor.submit(Callable {
            val startDate = System.currentTimeMillis()
            logger.info("New feed started building")
            val feed = buildFeedItem(entry)
            logger.info("Feed '${feed.title}' is built successful, downloading audio ...")
            val feedItemWithFile = ffmpegEncoder.downloadAndCompressMp3(feed)
            logger.info("$current/$total files downloaded and compressed on ${(System.currentTimeMillis() - startDate) / 1000} sec.")
            return@Callable feedItemWithFile
        })

    private companion object : KLogging() {
        const val DEFAULT_AUTHORS = "Umputun, Bobuk, Gray, Ksenks"
    }
}