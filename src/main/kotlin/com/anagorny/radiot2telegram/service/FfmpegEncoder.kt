package com.anagorny.radiot2telegram.service

import com.anagorny.radiot2telegram.helpers.removeFile
import com.anagorny.radiot2telegram.model.AudioMetaInfo
import com.anagorny.radiot2telegram.model.FeedItem
import com.anagorny.radiot2telegram.model.FeedItemWithFile
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit


@Component
class FfmpegEncoder {
    private val logger = LoggerFactory.getLogger(FfmpegEncoder::class.java)

    @Autowired
    lateinit var ffmpegTaskExecutor: FFmpegExecutor

    @Autowired
    lateinit var ffprobe: FFprobe

    @Value("\${radio_t_feeder.ffmpeg.work.dir}")
    private lateinit var workDir: String

    @Value("\${radio_t_feeder.download.retry.times}")
    private var retryTimes = 1

    @Value("\${radio_t_feeder.download.retry.timeout.ms}")
    private var retryTimeout: Long = 1L


    @Value("\${radio_t_feeder.download.connection.timeout}")
    private var connectionTimeout = 60000

    @Value("\${radio_t_feeder.download.read.timeout}")
    private var readTimeout = 60000

    @Value("\${radio_t_feeder.ffmpeg.mp3.bitrate}")
    private var mp3Bitrate: Long = 32L // at 32 kbit/s


    @Value("\${radio_t_feeder.ffmpeg.mp3.channels}")
    private var mp3Channels = 1 //Mono


    @Value("\${radio_t_feeder.ffmpeg.mp3.rate}")
    private var mp3Rate = 48_000 // at 48KHz

    @Value("\${radio_t_feeder.telegram.file.limit_in_bytes}")
    private var fileLimitBytes = 50000000 // ~ 47MB


    fun downloadAndCompressMp3(feedItem: FeedItem): FeedItemWithFile {
        feedItem.audioUrl ?: feedItem.audioUrlAlter
        ?: throw RuntimeException("Cannot download audio file for podcast '${feedItem.title}' because all audio url is null")

        var downloadedFile: File? = null

        for (index in 1..retryTimes) {
            if (feedItem.audioUrl != null) {
                downloadedFile = downloadMp3(feedItem.audioUrl)
            }
            if (downloadedFile == null) {
                if (feedItem.audioUrlAlter != null && feedItem.audioUrl != feedItem.audioUrlAlter) {
                    logger.info("Cant read file from '${feedItem.audioUrl}', but have alter url='${feedItem.audioUrlAlter}', downloading...")
                    downloadedFile = downloadMp3(feedItem.audioUrlAlter)
                }
            }
            if (downloadedFile != null) {
                break
            }
            logger.error("Error while downloading file for '${feedItem.title}', tryies used $index/$retryTimes")
            TimeUnit.MILLISECONDS.sleep(retryTimeout)
        }

        if (downloadedFile == null) {
            throw RuntimeException("Cannot download audio file for podcast '${feedItem.title}' because cant read file (downloaded file is null)")
        }

        val srcFilePath = downloadedFile.absolutePath
        logger.info("Source(no-compress) file downloaded to $srcFilePath...")

        val srcMetaInfo = fetchMetaData(downloadedFile)
        logger.info("Metadata for '$srcFilePath' successfully fetching.")

        val outFilePath = compressMp3(srcFilePath)
        if (srcFilePath != outFilePath) {
            removeFile(File(srcFilePath), logger)
            logger.info("Source file '$srcFilePath' successfully removed.")
        }
        return FeedItemWithFile(feedItem, outFilePath, srcMetaInfo)
    }

    fun downloadMp3(fileUrl: String): File? {
        val url = URL(fileUrl)
        val fileName = url.file.split("/").last()
        val srcFilePath = "$workDir/$fileName"
        return try {
            FileUtils.copyURLToFile(
                    url,
                    File(srcFilePath),
                    connectionTimeout,
                    readTimeout)

            File(srcFilePath)
        } catch (e: Exception) {
            logger.error("Error while download file'$fileUrl'", e)
            null
        }
    }

    fun fetchMetaData(srcFile: File): AudioMetaInfo {
        val srcFfprobeResult = ffprobe.probe(srcFile.absolutePath)?.format
        return AudioMetaInfo(
                title = srcFfprobeResult?.tags?.get("title"),
                artist = srcFfprobeResult?.tags?.get("artist"),
                album = srcFfprobeResult?.tags?.get("album"),
                genre = srcFfprobeResult?.tags?.get("genre"),
                duration = srcFfprobeResult?.duration?.toInt() ?: 0
        )
    }

    fun compressMp3(srcPath: String): String {

        val srcFfprobeResult = ffprobe.probe(srcPath).format

        val origBitRate = srcFfprobeResult.bit_rate
        val origFileSize = srcFfprobeResult.size

        if (origFileSize > fileLimitBytes) {
            val rate = ((fileLimitBytes / 1000) * 1000.0) / origFileSize
            val newBitRate = (origBitRate * rate / 1000).toLong() * 1000

            logger.info("File '$srcPath' to many big, using compression with rate=$rate")
            val outFilePath = srcPath.replace(".mp3", "-${newBitRate / 1000}kbps.mp3")

            val builder = FFmpegBuilder()
                    .setInput(srcPath)
                    .overrideOutputFiles(true)
                    .addOutput(outFilePath)
                    .setAudioCodec("mp3")
                    .setAudioBitRate(newBitRate)
                    .done()
            ffmpegTaskExecutor.createJob(builder).run()

            val compressFfprobeResult = ffprobe.probe(outFilePath).format

            val compressBitRate = compressFfprobeResult.bit_rate
            val compressFileSize = compressFfprobeResult.size

            logger.info("Use compressed file $outFilePath with size=${compressFileSize / 1024 / 1024}MB and bitrate=$compressBitRate")
            return outFilePath
        } else {
            logger.info("Use original file $srcPath with size=${origFileSize / 1024 / 1024}MB and bitrate=$origBitRate")
            return srcPath
        }
    }

}