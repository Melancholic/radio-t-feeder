package com.anagorny.rssreader.config

import com.anagorny.rssreader.model.MetaInfoContainer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@ComponentScan("com.anagorny.rssreader")
@EnableScheduling
open class SpringContext {

    @Value("\${radio_t_feeder.parallel.coresize}")
    private var coreSize: Int = 2

    @Value("\${radio_t_feeder.parallel.maxsize}")
    private var maxSize: Int = 4

    @Value("\${radio_t_feeder.metadata.file}")
    private lateinit var metaDataSrc: String


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



}