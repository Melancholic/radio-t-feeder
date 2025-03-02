package com.anagorny.radiot2telegram.services.impl

import com.anagorny.radiot2telegram.config.TelegramProperties
import com.anagorny.radiot2telegram.services.IAudioCompressor
import mu.KLogging
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import org.springframework.stereotype.Service
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit

@Service
class AudioCompressor(
    private val ffmpegTaskExecutor: FFmpegExecutor,
    private val ffprobe: FFprobe,
    private val properties: TelegramProperties
) : IAudioCompressor {

    override fun compressAudio(srcPath: String): String {
        val srcFfprobeResult = ffprobe.probe(srcPath)
        val srcFfprobeFormat = srcFfprobeResult.format

        val origBitRate = srcFfprobeFormat.bit_rate
        val origFileSize = DataSize.of(srcFfprobeFormat.size, DataUnit.BYTES)
        val maxSizeLimit = properties.files.audioMaxSize

        if (origFileSize > maxSizeLimit) {
            logger.info("File '$srcPath' too large (${origFileSize.toMegabytes()}MB), will be used compression to ${maxSizeLimit.toMegabytes()}MB")
            val outFilePath = srcPath.replace(".mp3", "-compressed.m4a")
            logger.info { "Compression (${origFileSize.toMegabytes()}MB => ${maxSizeLimit.toMegabytes()}MB) starting..." }

            val compressFfprobeResult = performFfmpegExecution(srcPath, outFilePath, maxSizeLimit).format
            val compressBitRate = compressFfprobeResult.bit_rate
            val compressFileSize = DataSize.of(compressFfprobeResult.size, DataUnit.BYTES)
            logger.info { "Compression finished with result: ${compressFileSize.toMegabytes()}MB/${maxSizeLimit.toMegabytes()}MB" }

            logger.info("Use compressed file $outFilePath with size=${compressFileSize.toMegabytes()}MB and bitrate=$compressBitRate")
            return outFilePath
        } else {
            logger.info("Use original file $srcPath with size=${origFileSize.toMegabytes()}MB and bitrate=$origBitRate")
            return srcPath
        }
    }

    private fun performFfmpegExecution(source: String, target: String, maxSizeLimit: DataSize): FFmpegProbeResult {
        val srcFfprobeResult = ffprobe.probe(source)
        val builder = FFmpegBuilder()
            .setInput(srcFfprobeResult)
            .overrideOutputFiles(true)
            .addOutput(target)
            .setAudioCodec("aac")
            .setTargetSize(maxSizeLimit.toBytes())
            .disableVideo()
            .done()
        ffmpegTaskExecutor.createJob(builder).run()

        return ffprobe.probe(target)
    }

    private companion object : KLogging()
}