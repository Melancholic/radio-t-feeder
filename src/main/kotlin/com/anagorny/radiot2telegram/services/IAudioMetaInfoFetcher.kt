package com.anagorny.radiot2telegram.services

import com.anagorny.radiot2telegram.model.AudioMetaInfo
import org.springframework.stereotype.Component
import java.io.File

@Component
interface IAudioMetaInfoFetcher {
    fun fetchMetaData(srcFile: File): AudioMetaInfo
}