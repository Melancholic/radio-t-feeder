package com.anagorny.radiot2telegram.config

import com.anagorny.radiot2telegram.model.MetaInfoContainer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rometools.rome.io.SyndFeedInput
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.client.RestTemplate
import org.telegram.telegrambots.bots.DefaultBotOptions


@Configuration
class SpringConfiguration {

    @Bean
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        return restTemplateBuilder.build()
    }

    @Bean
    fun threadPoolTaskExecutor(properties: ConcurrencyProperties): AsyncTaskExecutor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = properties.coreSize
            maxPoolSize = properties.maxSize
        }
    }

    @Bean
    fun jsonMapper(): ObjectMapper = ObjectMapper()
        .registerModule(
            KotlinModule.Builder()
                .build()
        )

    @Bean
    fun metaInfoContainer(mapper: ObjectMapper, properties: SystemProperties) =
        MetaInfoContainer(properties.metadataPath, mapper)


    @Bean
    fun ffmpeg(properties: MediaProperties) = FFmpeg(properties.ffmpeg.path)

    @Bean
    fun ffprobe(properties: MediaProperties) = FFprobe(properties.ffprobe.path)

    @Bean
    fun ffmpegTaskExecutor(ffmpeg: FFmpeg, ffprobe: FFprobe) = FFmpegExecutor(ffmpeg, ffprobe)

    @Bean
    fun syndFeedInput() = SyndFeedInput()

    @Bean
    fun defaultBotOptions(properties: TelegramProperties) : DefaultBotOptions {
        val options = DefaultBotOptions().apply {
            baseUrl = properties.serverUrl
        }

        return options
    }
}