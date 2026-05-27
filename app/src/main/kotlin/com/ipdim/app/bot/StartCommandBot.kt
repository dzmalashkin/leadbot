package com.ipdim.app.bot

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient

@Component
@ConditionalOnProperty(prefix = "telegram.bot", name = ["token"])
class StartCommandBot(
    @param:Value($$"${telegram.bot.token}") private val botToken: String,
) : SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private val log = LoggerFactory.getLogger(StartCommandBot::class.java)
    private val telegramClient: TelegramClient = OkHttpTelegramClient(botToken)

    override fun getBotToken(): String = botToken

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(update: Update) {
        val message = update.message ?: return
        val text = message.text ?: return

        if (text == "/start") {
            sendMessage(message.chatId, "Привет")
        }
    }

    private fun sendMessage(chatId: Long, text: String) {
        val message = SendMessage.builder()
            .chatId(chatId)
            .text(text)
            .build()

        try {
            telegramClient.execute(message)
        } catch (exception: TelegramApiException) {
            log.warn("Failed to send Telegram message", exception)
        }
    }
}
