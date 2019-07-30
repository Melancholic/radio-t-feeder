package com.anagorny.rssreader.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendAudio
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.File
import java.net.URL


@Component
class TelegramBot : TelegramLongPollingBot() {
    private val logger = LoggerFactory.getLogger(TelegramBot::class.java)


    @Value("\${radio_t_feeder.telegram.chat_id}")
    private lateinit var tgСhannelIdentifier: String

    @Value("\${radio_t_feeder.telegram.bot.token}")
    private lateinit var tgBotToken: String

    @Value("\${radio_t_feeder.telegram.bot.name}")
    private lateinit var tgBotName: String


    override fun onUpdateReceived(update: Update) {

//        // We check if the update has a message and the message has text
//        if (update.hasMessage() && update.message.hasText()) {
//            // Set variables
//            val message_text = update.message.text
//            val chat_id = update.message.chatId!!
//            val message = SendMessage() // Create a message object object
//                    .setChatId(chat_id)
//                    .setText(message_text)
////            try {
////                sendMessage(message) // Sending our message object to user
////            } catch (e: TelegramApiException) {
////                e.printStackTrace()
////            }
//
//        }
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
        //TODO retrying policy
        return execute(sendAudioRequest)
    }
}