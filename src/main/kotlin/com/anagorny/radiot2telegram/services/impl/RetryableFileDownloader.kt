package com.anagorny.radiot2telegram.services.impl

import com.anagorny.radiot2telegram.config.DownloadProperties
import com.anagorny.radiot2telegram.config.MediaProperties
import com.anagorny.radiot2telegram.services.FileDownloader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
@Qualifier("retryableFileDownloader")
open class RetryableFileDownloader(
    mediaProperties: MediaProperties,
    downloadProperties: DownloadProperties,
) : FileDownloader(mediaProperties, downloadProperties) {

    override val logger = LoggerFactory.getLogger(RetryableFileDownloader::class.java)

    @Retryable(
        maxAttemptsExpression = "\${download.retry.max-attempts}",
        backoff = Backoff(delayExpression = "\${download.retry.delay}")
    )
    override fun download(fileUrl: String, mirror: String?) = super.download(fileUrl, mirror)
}