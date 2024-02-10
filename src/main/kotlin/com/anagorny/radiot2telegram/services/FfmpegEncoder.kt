package com.anagorny.radiot2telegram.services

import com.anagorny.radiot2telegram.helpers.removeFile
import com.anagorny.radiot2telegram.model.AudioMetaInfo
import com.anagorny.radiot2telegram.model.FeedItem
import com.anagorny.radiot2telegram.model.FeedItemWithFile
import com.anagorny.radiot2telegram.services.impl.JPGExtensionChecker
import mu.KLogging
import org.springframework.stereotype.Service
import java.io.File


@Service
class FfmpegEncoder(
    private val retryableFileDownloader: IDownloader,
    private val fileDownloader: IDownloader,
    private val audioCompressor: IAudioCompressor,
    private val audioMetaInfoFetcher: IAudioMetaInfoFetcher,
    private val jpgExtensionChecker: JPGExtensionChecker,
    private val jpegConverter: IConverter
) {

    fun downloadAndCompressMp3(feedItem: FeedItem): FeedItemWithFile {
        val thumbFilePath = doProcessingThumb(feedItem)
        val (outFilePath, srcMetaInfo) = doProcessingAudio(feedItem)

        return FeedItemWithFile(feedItem, outFilePath, srcMetaInfo, thumbFilePath)
    }

    protected fun doProcessingAudio(feedItem: FeedItem): Pair<String, AudioMetaInfo> {
        val downloadedFile = try {
            retryableFileDownloader.download(feedItem.audioUrl, feedItem.audioUrlAlter)
        } catch (e: java.lang.Exception) {
            throw RuntimeException(
                "Cannot download audio file for podcast '${feedItem.title}' because all tries were unsuccessful.", e
            )
        }

        val srcFilePath = downloadedFile.file.absolutePath
        logger.info("Source(no-compressed) file downloaded to $srcFilePath...")

        val srcMetaInfo = audioMetaInfoFetcher.fetchMetaData(downloadedFile.file)
        logger.info("Metadata for '$srcFilePath' successfully fetching.")

        val outFilePath = audioCompressor.compressAudio(srcFilePath)
        if (srcFilePath != outFilePath) {
            removeFile(File(srcFilePath), logger)
            logger.info("Source file '$srcFilePath' successfully removed.")
        }
        return Pair(outFilePath, srcMetaInfo)
    }

    protected fun doProcessingThumb(feedItem: FeedItem): String? {
        if (feedItem.thumbUrl.isNullOrBlank()) return null
        val thumbOutFile =
            try {
                fileDownloader.download(feedItem.thumbUrl, null)
            } catch (e: Exception) {
                logger.error("Cant download thumb file: ${feedItem.thumbUrl}", e)
                return null
            }
        logger.info("Source(no-converted) file ${thumbOutFile.fullName} downloaded to ${thumbOutFile.file.absoluteFile}...")
        return if (jpgExtensionChecker.check(thumbOutFile.fullName)) {
            thumbOutFile.file.absolutePath
        } else {
            try {
                jpegConverter.convert(thumbOutFile).file.absolutePath
            } catch (e: Exception) {
                return null
            }
        }
    }

    private companion object : KLogging()
}