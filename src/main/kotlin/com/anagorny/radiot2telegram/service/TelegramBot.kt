package com.anagorny.radiot2telegram.service

import com.anagorny.radiot2telegram.model.FeedItemWithFile
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

    private fun loadFile(filePath: String): File {
        return try {
            File(filePath)
        } catch (e: Exception) {
            logger.error("Cant read file '$filePath' fo sending", e)
            throw e
        }
    }

    fun sendAudio(feedItemWithFile: FeedItemWithFile): Message? {
        val (feed, filePath, audioMetaDataInfo) = feedItemWithFile
        val file = loadFile(filePath)
        val sendAudioRequest = SendAudio()
        sendAudioRequest.chatId = "@$tgСhannelIdentifier"
        sendAudioRequest.caption = feed.description
        sendAudioRequest.parseMode = "HTML"
        sendAudioRequest.setAudio(file)
        sendAudioRequest.performer = audioMetaDataInfo.artist ?: "Umputun, Bobuk, Gray, Ksenks"
        sendAudioRequest.title = feed.title
        sendAudioRequest.duration = audioMetaDataInfo.duration

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