package com.anagorny.radiot2telegram.services.impl

import com.anagorny.radiot2telegram.model.AudioMetaInfo
import com.anagorny.radiot2telegram.services.IAudioMetaInfoFetcher
import net.bramp.ffmpeg.FFprobe
import java.io.File

@org.springframework.stereotype.Service
class AudioMetaInfoFetcher(
    private val ffprobe: FFprobe,
) : IAudioMetaInfoFetcher {
    override fun fetchMetaData(srcFile: File): AudioMetaInfo {
        val srcFfprobeResult = ffprobe.probe(srcFile.absolutePath)?.format
        return AudioMetaInfo(
            title = srcFfprobeResult?.tags?.get("title"),
            artist = srcFfprobeResult?.tags?.get("artist"),
            album = srcFfprobeResult?.tags?.get("album"),
            genre = srcFfprobeResult?.tags?.get("genre"),
            duration = srcFfprobeResult?.duration?.toInt() ?: 0
        )
    }
}