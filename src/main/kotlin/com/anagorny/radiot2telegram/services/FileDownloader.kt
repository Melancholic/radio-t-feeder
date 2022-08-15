package com.anagorny.radiot2telegram.services

import com.anagorny.radiot2telegram.config.DownloadProperties
import com.anagorny.radiot2telegram.config.MediaProperties
import com.anagorny.radiot2telegram.model.FileContainer
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.net.URL

@Service
open class FileDownloader(
    private val mediaProperties: MediaProperties,
    private val downloadProperties: DownloadProperties,
) : IDownloader {

    protected val logger = LoggerFactory.getLogger(FileDownloader::class.java)

    override fun download(fileUrl: String, mirror: String?): FileContainer {
        val (file, url) = try {
            doDownload(fileUrl)
        } catch (e: java.lang.Exception) {
            if (mirror.isNullOrEmpty()) throw e
            doDownload(mirror)
        }
        val baseName = getBaseName(url.path)
        val extension = getExtension(url.path)
        return FileContainer(file, baseName, extension)
    }

    protected open fun doDownload(fileUrl: String): Pair<File, URL> {
        val url = URL(fileUrl)
        val fileName = getName(url.path)
        val srcFilePath = "${mediaProperties.workDir}/$fileName"
        val file = File(srcFilePath)

        try {
            FileUtils.copyURLToFile(
                url,
                file,
                downloadProperties.connectionTimeout.toMillis().toInt(),
                downloadProperties.readTimeout.toMillis().toInt()
            )
        } catch (e: Exception) {
            logger.error("File download error from '$fileUrl'", e)
            throw e
        }
        return (file to url)
    }
}