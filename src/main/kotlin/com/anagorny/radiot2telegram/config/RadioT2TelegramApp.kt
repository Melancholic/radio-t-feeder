package com.anagorny.radiot2telegram.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.telegram.telegrambots.ApiContextInitializer


@SpringBootApplication
class RadioT2TelegramApp

fun main(args: Array<String>) {
	ApiContextInitializer.init()
	runApplication<RadioT2TelegramApp>(*args)
}

