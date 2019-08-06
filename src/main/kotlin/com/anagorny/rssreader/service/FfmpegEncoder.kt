package com.anagorny.rssreader.service

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
    private val logger = LoggerFactory.getLogger(Feeder::class.java)

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


    fun downloadAndCompressMp3(feedItem: FeedItem): File {
        val srcUrl = feedItem.audioUrl ?: feedItem.audioUrlAlter
        ?: throw RuntimeException("Cannot download audio file for podcast '${feedItem.title}' because all audio url is null")

        var dowloadedFile: File? = null

        for (index in 1..retryTimes) {
            dowloadedFile = downloadMp3(srcUrl)
            if (dowloadedFile == null) {
                if (feedItem.audioUrlAlter != null && srcUrl != feedItem.audioUrlAlter) {
                    logger.info("Cant read file from '$srcUrl', but have alter url='${feedItem.audioUrlAlter}', downloading...")
                    dowloadedFile = downloadMp3(feedItem.audioUrlAlter)
                }
            }
            if (dowloadedFile != null) {
                break
            }
            logger.error("Error while downloading file for '${feedItem.title}', tryies used $index/$retryTimes")
            TimeUnit.MILLISECONDS.sleep(retryTimeout)
        }

        if (dowloadedFile == null) throw RuntimeException("Cannot download audio file for podcast '${feedItem.title}' because cant read file (downloaded file is null)")

        val fileName = dowloadedFile.name
        val srcFilePath = dowloadedFile.absolutePath

        logger.info("Source(no-compress) file downloaded to $srcFilePath...")
        val outFileName = fileName.replace(".mp3", "-32kbps.mp3")
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
                .setAudioChannels(1)         // Mono audio
                .setAudioCodec("mp3")        // using the aac codec
                .setAudioSampleRate(48_000)  // at 48KHz
                .setAudioBitRate(32768)      // at 32 kbit/s
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