package com.anagorny.radiot2telegram

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableRetry
@EnableAsync
class RadioT2TelegramApp

fun main(args: Array<String>) {
	runApplication<RadioT2TelegramApp>(*args)
}

