package com.anagorny.rssreader.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.telegram.telegrambots.ApiContextInitializer


@SpringBootApplication
class RssReaderApplication

fun main(args: Array<String>) {
	ApiContextInitializer.init()
	runApplication<RssReaderApplication>(*args)
}

