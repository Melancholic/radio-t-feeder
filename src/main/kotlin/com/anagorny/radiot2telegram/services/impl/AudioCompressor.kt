package com.anagorny.radiot2telegram.services.impl

import com.anagorny.radiot2telegram.config.TelegramProperties
import com.anagorny.radiot2telegram.services.IAudioCompressor
import mu.KLogging
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.springframework.stereotype.Service

@Service
class AudioCompressor(
    private val ffmpegTaskExecutor: FFmpegExecutor,
    private val ffprobe: FFprobe,
    private val properties: TelegramProperties
) : IAudioCompressor {

    override fun compressAudio(srcPath: String): String {

        val srcFfprobeResult = ffprobe.probe(srcPath).format

        val origBitRate = srcFfprobeResult.bit_rate
        val origFileSize = srcFfprobeResult.size

        if (origFileSize > properties.files.audioMaxSize.toBytes()) {
            val rate = ((properties.files.audioMaxSize.toBytes() / 1024) * 1024.0) / origFileSize
            val newBitRate = (origBitRate * rate / 1024).toLong() * 1024

            logger.info("File '$srcPath' too many big, using compression with rate=$rate")
            val outFilePath = srcPath.replace(".mp3", "-${newBitRate / 1024}kbps.mp3")

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

    private companion object : KLogging()
}