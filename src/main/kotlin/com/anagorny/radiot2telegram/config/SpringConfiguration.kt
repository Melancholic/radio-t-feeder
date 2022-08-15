package com.anagorny.radiot2telegram.config

import com.anagorny.radiot2telegram.model.MetaInfoContainer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@Import(JobConfig::class)
open class SpringConfiguration {

    @Bean
    fun threadPoolTaskExecutor(properties: ConcurrencyProperties): AsyncListenableTaskExecutor {
        val threadPoolTaskExecutor = ThreadPoolTaskExecutor()
        threadPoolTaskExecutor.corePoolSize = properties.coreSize
        threadPoolTaskExecutor.maxPoolSize = properties.maxSize
        return threadPoolTaskExecutor
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


}