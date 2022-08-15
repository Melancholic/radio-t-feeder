package com.anagorny.radiot2telegram.services

import com.anagorny.radiot2telegram.model.FileContainer
import org.springframework.stereotype.Component

@Component
interface IDownloader {
    fun download(fileUrl: String, mirror: String?): FileContainer
}