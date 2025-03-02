package com.anagorny.radiot2telegram.services

import com.anagorny.radiot2telegram.config.TelegramProperties
import com.anagorny.radiot2telegram.model.FeedItemWithFile
import mu.KLogging
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendAudio
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.File


@Component
class TelegramBot(
    private val telegramProperties: TelegramProperties,
    options: DefaultBotOptions
) : TelegramLongPollingBot(options, telegramProperties.bot.token) {

    override fun onUpdateReceived(update: Update) {
        TODO()
    }

    override fun getBotUsername() = telegramProperties.bot.name

    private fun loadFile(filePath: String): InputFile {
        return try {
            InputFile(File(filePath))
        } catch (e: Exception) {
            logger.error("Cant read file '$filePath' fo sending", e)
            throw e
        }
    }

    fun sendAudio(feedItemWithFile: FeedItemWithFile): Message? {
        val (feed, audioFilePath, audioMetaDataInfo, thumbFilePath) = feedItemWithFile
        val audioFile = loadFile(audioFilePath)
        val sendAudioRequest = SendAudio()
        sendAudioRequest.chatId = "@${telegramProperties.chatId}"
        sendAudioRequest.caption = feed.description
        sendAudioRequest.parseMode = "HTML"
        sendAudioRequest.audio = audioFile
        sendAudioRequest.performer = audioMetaDataInfo.artist ?: "Umputun, Bobuk, Gray, Ksenks"
        sendAudioRequest.title = feed.title
        sendAudioRequest.duration = audioMetaDataInfo.duration

        if (thumbFilePath != null) {
            sendAudioRequest.thumbnail = loadFile(thumbFilePath)
        }

        return try {
            doSendMessage(sendAudioRequest)
        } catch (e: java.lang.Exception) {
            logger.error("I cant send message '${feed.title}'($feed) to telegram", e)
            if (!telegramProperties.sending.ignoreError) {
                throw RuntimeException("I cant send message '${feed.title}'($feed) to telegram", e)
            } else {
                return null
            }
        }
    }

    @Retryable(
        maxAttemptsExpression = "\${telegram.sending.retry.maxAttempts}",
        backoff = Backoff(delayExpression = "\${telegram.sending.retry.delay}")
    )
    fun doSendMessage(sendAudioRequest: SendAudio): Message {
        return execute(sendAudioRequest) ?: throw TelegramApiException("Message is null!")
    }

    private companion object : KLogging()
}