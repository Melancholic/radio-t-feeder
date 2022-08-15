package com.anagorny.radiot2telegram.services

import org.springframework.stereotype.Component

@Component
interface IAudioCompressor {
    fun compressAudio(srcPath: String): String
}
