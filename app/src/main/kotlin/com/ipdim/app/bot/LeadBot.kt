package com.ipdim.app.bot

import com.ipdim.app.dto.LeadRequestDto
import com.ipdim.app.service.LeadRequestConversationService
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
class LeadBot(
    @param:Value($$"${telegram.bot.token}") private val botToken: String,
    @param:Value($$"${telegram.bot.admin-chat-id:}") private val adminChatId: String,
    private val leadRequestConversationService: LeadRequestConversationService,
) : SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private val log = LoggerFactory.getLogger(LeadBot::class.java)
    private val telegramClient: TelegramClient = OkHttpTelegramClient(botToken)

    override fun getBotToken(): String = botToken

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(update: Update) {
        val message = update.message ?: return
        val text = message.text ?: return
        val chatId = message.chatId

        val result = if (text == "/start") {
            leadRequestConversationService.start(chatId)
        } else {
            leadRequestConversationService.handleText(chatId, text)
        }

        sendMessage(chatId, result.userReply)

        result.request?.let { request ->
            sendAdminNotification(request)
        }
    }

    private fun sendAdminNotification(request: LeadRequestDto) {
        val chatId = adminChatId.toLongOrNull()

        if (chatId == null) {
            log.warn("Telegram admin chat id is not configured")
            return
        }

        sendMessage(chatId, request.toAdminMessage())
    }

    private fun LeadRequestDto.toAdminMessage(): String = """
        Новая заявка:
        Имя: $name
        Телефон: $phone
        Сообщение: $message
    """.trimIndent()

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
