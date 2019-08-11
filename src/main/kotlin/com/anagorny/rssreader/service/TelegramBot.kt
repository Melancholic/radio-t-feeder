package com.anagorny.rssreader.service

import com.anagorny.rssreader.model.FeedItem
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendAudio
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit


@Component
class TelegramBot : TelegramLongPollingBot() {
    private val logger = LoggerFactory.getLogger(TelegramBot::class.java)


    @Value("\${radio_t_feeder.telegram.chat_id}")
    private lateinit var tgСhannelIdentifier: String

    @Value("\${radio_t_feeder.telegram.bot.token}")
    private lateinit var tgBotToken: String

    @Value("\${radio_t_feeder.telegram.bot.name}")
    private lateinit var tgBotName: String


    @Value("\${radio_t_feeder.telegram.send.retry.times}")
    private var retryTimes = 1

    @Value("\${radio_t_feeder.telegram.send.retry.timeout.ms}")
    private var retryTimeout: Long = 1L

    @Value("\${radio_t_feeder.telegram.send.ignore.error}")
    private var ignoreError: Boolean = true

    override fun onUpdateReceived(update: Update) {
        TODO()
    }

    override fun getBotUsername(): String {
        // Return bot username
        // If bot username is @MyAmazingBot, it must return 'MyAmazingBot'
        return tgBotName
    }

    override fun getBotToken(): String {
        // Return bot token from BotFather
        return tgBotToken
    }

    fun sendAudio(file: File, feed: FeedItem) : Message? {
        // Create send method
        val sendAudioRequest = SendAudio()
        // Set destination chat id
        sendAudioRequest.chatId = "@$tgСhannelIdentifier"
        sendAudioRequest.caption = feed.description
        sendAudioRequest.parseMode = "HTML"
        sendAudioRequest.setAudio(file)
        //TODO meta from audiofile
        sendAudioRequest.performer = "Umputun, Bobuk, Gray, Ksenks"
        sendAudioRequest.title = feed.title

        if (!feed.thumbUrl.isNullOrBlank()) {
            try {
                sendAudioRequest.thumb = InputFile(URL(feed.thumbUrl).openStream(), feed.title)
            } catch (e: Exception) {
                logger.error("Cant download thumb file: ${feed.thumbUrl}", e)
            }
        }


        var exception: Exception? = null
        for (index in 1 .. retryTimes) {
            try {
                return execute(sendAudioRequest) ?: throw TelegramApiException("Mesage is null!")
            } catch (e : Exception) {
                exception = e
                logger.error("Error while sending message '${feed.title}', tryies used $index/$retryTimes", e)
                TimeUnit.MILLISECONDS.sleep(retryTimeout)
                continue
            }

        }

        logger.error("I cant sended message '${feed.title}'($feed) to telegram", exception)

        return if (ignoreError) {
            null
        } else {
            throw RuntimeException("I cant sended message '${feed.title}'($feed) to telegram", exception)
        }
    }
}