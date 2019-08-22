package com.anagorny.radiot2telegram.service

import com.anagorny.radiot2telegram.model.FeedItem
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit


@Component
class FfmpegEncoder {
    private val logger = LoggerFactory.getLogger(FfmpegEncoder::class.java)

    @Value("\${radio_t_feeder.ffmpeg.bin.path}")
    private lateinit var ffmpegSrc: String

    @Value("\${radio_t_feeder.ffmpeg.ffprobe.bin.path}")
    private lateinit var ffprobeSrc: String

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


    fun downloadAndCompressMp3(feedItem: FeedItem): File {
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

        val fileName = downloadedFile.name
        val srcFilePath = downloadedFile.absolutePath

        logger.info("Source(no-compress) file downloaded to $srcFilePath...")
        val outFileName = fileName.replace(".mp3", "-${mp3Bitrate}kbps.mp3")
        val outFilePath = "$workDir/$outFileName"
        compressMp3(srcFilePath, outFilePath)
        logger.info("Compressed file saved to $outFilePath.")
        removeFile(File(srcFilePath), logger)
        return File(outFilePath)
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

    fun compressMp3(srcPath: String, outPath: String) {
        val ffmpeg = FFmpeg(ffmpegSrc)
        val ffprobe = FFprobe(ffprobeSrc)

        val builder = FFmpegBuilder()
                .setInput(srcPath)
                .overrideOutputFiles(true)
                .addOutput(outPath)
                .setAudioChannels(mp3Channels)
                .setAudioCodec("mp3")
                .setAudioSampleRate(mp3Rate)
                .setAudioBitRate(mp3Bitrate * 1024)
                .done()
        val executor = FFmpegExecutor(ffmpeg, ffprobe)
        executor.createJob(builder).run()
    }

}

fun removeFile(file: File, logger: Logger) {
    try {
        val path = file.absolutePath
        if (file.delete()) {
            logger.info("File $path deleted successfully")
        } else {
            throw Exception("file $path cant be deleted")
        }
    } catch (e: Exception) {
        logger.error("Error while remove file", e)
    }
}