package com.anagorny.radiot2telegram.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DataSizeUnit
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import java.time.Duration

@ConfigurationProperties(prefix = "telegram")
data class TelegramProperties(
    val chatId: String,
    val bot: BotProperties,
    val sending: SendingProperties,
    val files: FilesProperties,
    val serverUrl: String = "https://api.telegram.org/bot"
) {
    data class BotProperties(
        val token: String,
        val name: String
    )

    data class SendingProperties(
        val ignoreError: Boolean,
        val retry: RetryConfiguration
    )

    data class FilesProperties(
        @DataSizeUnit(DataUnit.MEGABYTES)
        val audioMaxSize: DataSize,
    )
}

@ConfigurationProperties(prefix = "hashtags")
data class HashTagsSuggestionProperties(
    val enabled: Boolean,
    val count: Int,
    val api: ApiProperties
) {
    data class ApiProperties(
        val token: String,
        val url: String
    )
}

@ConfigurationProperties(prefix = "system")
data class SystemProperties(
    val workDir: String,
    val metadataPath: String
)

@ConfigurationProperties(prefix = "media")
data class MediaProperties(
    val workDir: String,
    val ffmpeg: ProgramProperties,
    val ffprobe: ProgramProperties,
) {
    data class ProgramProperties(
        val path: String
    )
}


@ConfigurationProperties(prefix = "parallel")
data class ConcurrencyProperties(
    val coreSize: Int,
    val maxSize: Int
)

@ConfigurationProperties(prefix = "rss")
data class RssProperties(
    val archive: ArchiveProperties,
    val main: MainProperties
) {
    data class ArchiveProperties(
        val url: String,
        val batchSize: Int
    )

    data class MainProperties(
        val url: String,
        val cron: List<String>
    )
}

@ConfigurationProperties(prefix = "download")
data class DownloadProperties(
    val retry: RetryConfiguration,
    val connectionTimeout: Duration,
    val readTimeout: Duration
)

data class RetryConfiguration(
    val maxAttempts: Int,
    val delay: Duration
)