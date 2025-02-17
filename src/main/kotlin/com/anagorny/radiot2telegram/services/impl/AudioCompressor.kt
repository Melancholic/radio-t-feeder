package com.anagorny.radiot2telegram.services.impl

import com.anagorny.radiot2telegram.config.TelegramProperties
import com.anagorny.radiot2telegram.services.IAudioCompressor
import mu.KLogging
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.springframework.stereotype.Service
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import kotlin.math.floor

@Service
class AudioCompressor(
    private val ffmpegTaskExecutor: FFmpegExecutor,
    private val ffprobe: FFprobe,
    private val properties: TelegramProperties
) : IAudioCompressor {

    override fun compressAudio(srcPath: String): String {

        val srcFfprobeResult = ffprobe.probe(srcPath).format

        val origBitRate = srcFfprobeResult.bit_rate
        val origFileSize = DataSize.of(srcFfprobeResult.size, DataUnit.BYTES)
        val maxSizeLimit = properties.files.audioMaxSize

        if (origFileSize > maxSizeLimit) {
            val rate = maxSizeLimit.toBytes() * 1.0 / origFileSize.toBytes()
            val newBitRate = floor(origBitRate * rate).toLong()

            logger.info("File '$srcPath' too many big (${origFileSize.toMegabytes()}MB), using compression with rate=$rate")
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
            val compressFileSize = DataSize.of(compressFfprobeResult.size, DataUnit.BYTES)

            logger.info("Use compressed file $outFilePath with size=${compressFileSize.toMegabytes()}MB and bitrate=$compressBitRate")
            return outFilePath
        } else {
            logger.info("Use original file $srcPath with size=${origFileSize.toMegabytes()}MB and bitrate=$origBitRate")
            return srcPath
        }
    }

    private companion object : KLogging()
}