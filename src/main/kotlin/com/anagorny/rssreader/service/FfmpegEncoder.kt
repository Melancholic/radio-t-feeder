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


@Component
class FfmpegEncoder {
    private val logger = LoggerFactory.getLogger(Feeder::class.java)

    @Value("\${radio_t_feeder.ffmpeg.bin.path}")
    private lateinit var ffmpegSrc: String

    @Value("\${radio_t_feeder.ffmpeg.ffprobe.bin.path}")
    private lateinit var ffprobeSrc: String

    @Value("\${radio_t_feeder.ffmpeg.work.dir}")
    private lateinit var workDir: String

    fun downloadAndCompressMp3(fileUrl: String) : File {
        val url = URL(fileUrl)
        val fileName = url.file.split("/").last()
        val srcFilePath = "$workDir/$fileName"

        FileUtils.copyURLToFile(
                url,
                File(srcFilePath),
                60000,
                60000)
        logger.info("Source(no-compress) file downloaded to $srcFilePath...")
        val outFileName = fileName.replace(".mp3", "-32kbps.mp3")
        val outFilePath = "$workDir/$outFileName"
        compressMp3(srcFilePath, outFilePath)
        logger.info("Compressed file saved to $outFilePath.")
        removeFile(File(srcFilePath), logger)
        return File(outFilePath)
    }

    fun compressMp3(srcPath: String, outPath: String) {
        val ffmpeg = FFmpeg(ffmpegSrc)
        val ffprobe = FFprobe(ffprobeSrc)

        val  builder = FFmpegBuilder()
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
        logger.error("Error while remove file ", e)
    }
}