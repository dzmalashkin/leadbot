package com.ipdim.app.bot

import com.ipdim.app.dto.LeadRequestDto
import com.ipdim.app.service.ConversationResult
import com.ipdim.app.service.LeadRequestAdminMessageFormatter
import com.ipdim.app.service.LeadRequestConversationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient

@Component
@ConditionalOnExpression("'\${telegram.bot.token:}' != ''")
class LeadBot(
    @param:Value($$"${telegram.bot.token}") private val botToken: String,
    @param:Value($$"${telegram.bot.admin-chat-id:}") private val adminChatId: String,
    @param:Value($$"${telegram.bot.order-chat-id:}") private val orderChatId: String,
    @param:Value($$"${telegram.bot.demo:false}") private val demoMode: Boolean,
    private val leadRequestConversationService: LeadRequestConversationService,
    private val leadRequestAdminMessageFormatter: LeadRequestAdminMessageFormatter,
) : SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private val log = LoggerFactory.getLogger(LeadBot::class.java)
    private val telegramClient: TelegramClient = OkHttpTelegramClient(botToken)

    override fun getBotToken(): String = botToken

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(update: Update) {
        val message = update.message ?: return
        val text = message.text ?: return
        val chatId = message.chatId

        val result = when (text) {
            "/start" -> leadRequestConversationService.start(chatId)
            "/cancel" -> leadRequestConversationService.cancel(chatId)
            TRY_DEMO_BUTTON -> leadRequestConversationService.startDemo(chatId)
            ORDER_BUTTON -> handleOrderRequest(chatId, message.from)
            else -> leadRequestConversationService.handleText(chatId, text)
        }

        sendMessage(chatId, result)

        result.request?.let { request ->
            sendAdminNotification(request)
        }
    }

    private fun handleOrderRequest(chatId: Long, user: User?): ConversationResult {
        if (!demoMode) {
            return leadRequestConversationService.handleText(chatId, ORDER_BUTTON)
        }

        sendOrderNotification(chatId, user)

        return ConversationResult.reply(
            """
            ✅ Request sent.

            The developer has received your contact info and will get back to you soon.
            """.trimIndent(),
            leadRequestConversationService.removeKeyboard(),
        )
    }

    private fun sendAdminNotification(request: LeadRequestDto) {
        val chatId = adminChatId.toLongOrNull()

        if (chatId == null) {
            log.warn("Telegram admin chat id is not configured")
            return
        }

        sendMessage(chatId, leadRequestAdminMessageFormatter.format(request))
    }

    private fun sendOrderNotification(chatId: Long, user: User?) {
        val targetChatId = orderChatId.toLongOrNull() ?: adminChatId.toLongOrNull()

        if (targetChatId == null) {
            log.warn("Telegram order chat id is not configured")
            return
        }

        sendMessage(targetChatId, orderMessage(chatId, user))
    }

    private fun orderMessage(chatId: Long, user: User?): String = """
        🔥 New order request

        User: ${username(user)}
        Name: ${fullName(user)}
        Chat ID: $chatId
    """.trimIndent()

    private fun username(user: User?): String =
        user?.userName
            ?.takeIf { it.isNotBlank() }
            ?.let { "@$it" }
            ?: "No username"

    private fun fullName(user: User?): String {
        val nameParts = listOfNotNull(user?.firstName, user?.lastName)
            .filter { it.isNotBlank() }

        return nameParts.joinToString(" ").ifBlank { "No name" }
    }

    private fun sendMessage(chatId: Long, result: ConversationResult) {
        val message = SendMessage.builder()
            .chatId(chatId)
            .text(result.userReply)
            .replyMarkup(result.replyMarkup)
            .build()

        try {
            telegramClient.execute(message)
        } catch (exception: TelegramApiException) {
            log.warn("Failed to send Telegram message", exception)
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

    private companion object {
        const val TRY_DEMO_BUTTON = "📝 Try demo"
        const val ORDER_BUTTON = "🚀 Order a similar bot"
    }
}
