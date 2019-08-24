package com.anagorny.radiot2telegram.service

import com.anagorny.radiot2telegram.model.FeedItem
import com.anagorny.radiot2telegram.model.FeedItemWithFile
import com.anagorny.radiot2telegram.model.MetaInfoContainer
import com.rometools.rome.feed.synd.SyndEntry
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.AsyncListenableTaskExecutor
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Future

abstract class AbstractFeederService {
    @Autowired
    open lateinit var threadPoolTaskExecutor: AsyncListenableTaskExecutor
    @Autowired
    open lateinit var ffmpegEncoder: FfmpegEncoder
    @Autowired
    protected lateinit var feedFetcher: FeedFetcher
    @Autowired
    protected lateinit var metaInfoContainer: MetaInfoContainer

    abstract val logger: Logger

    protected fun getMostRecentNews(feedUrl: String): List<SyndEntry> {
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

    protected fun buildFeedItem(entry: SyndEntry): FeedItem {
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


    protected fun asyncPreparingFile(entry: SyndEntry, total: Int, current: Int): Future<FeedItemWithFile> = threadPoolTaskExecutor.submit(Callable<FeedItemWithFile> {
        val startDate = System.currentTimeMillis()
        val feed = buildFeedItem(entry)
        logger.info("Feed '${feed.title}' is builded, downloading...")
        val filePath = ffmpegEncoder.downloadAndCompressMp3(feed)
        logger.info("$current/$total files downloaded and compressed on ${(System.currentTimeMillis() - startDate) / 1000} sec")
        return@Callable FeedItemWithFile(item = feed, filePath = filePath)
    })
}