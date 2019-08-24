package com.anagorny.radiot2telegram.config

import com.anagorny.radiot2telegram.model.MetaInfoContainer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@ComponentScan("com.anagorny.radiot2telegram")
@EnableScheduling
@Import(JobConfig::class)
open class SpringContext {

    @Value("\${radio_t_feeder.parallel.coresize}")
    private var coreSize: Int = 2

    @Value("\${radio_t_feeder.parallel.maxsize}")
    private var maxSize: Int = 4

    @Value("\${radio_t_feeder.metadata.file}")
    private lateinit var metaDataSrc: String
    @Value("\${radio_t_feeder.ffmpeg.bin.path}")
    private lateinit var ffmpegSrc: String

    @Value("\${radio_t_feeder.ffmpeg.ffprobe.bin.path}")
    private lateinit var ffprobeSrc: String



    @Bean
    fun threadPoolTaskExecutor(): AsyncListenableTaskExecutor {
        val threadPoolTaskExecutor = ThreadPoolTaskExecutor()
        threadPoolTaskExecutor.corePoolSize = coreSize
        threadPoolTaskExecutor.maxPoolSize = maxSize
        return threadPoolTaskExecutor
    }

    @Bean
    fun jsonMapper(): ObjectMapper = ObjectMapper()
            .registerModule(KotlinModule())

    @Bean
    fun metaInfoContainer(jsonMapper: ObjectMapper) = MetaInfoContainer(metaDataSrc, jsonMapper)


    @Bean
    fun ffmpeg() = FFmpeg(ffmpegSrc)

    @Bean
    fun ffprobe() = FFprobe(ffprobeSrc)

    @Bean
    fun ffmpegTaskExecutor(ffmpeg: FFmpeg, ffprobe: FFprobe) = FFmpegExecutor(ffmpeg, ffprobe)


}