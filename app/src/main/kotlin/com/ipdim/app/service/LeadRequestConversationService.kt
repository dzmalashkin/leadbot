package com.ipdim.app.service

import com.ipdim.app.dto.LeadRequestDto
import com.ipdim.app.model.LeadState
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class LeadRequestConversationService(
    private val leadRequestMemoryStorage: LeadRequestMemoryStorage,
    private val phoneValidator: PhoneValidator,
    private val leadRequestAdminMessageFormatter: LeadRequestAdminMessageFormatter,
    @param:Value($$"${telegram.bot.demo:false}") private val demoMode: Boolean,
) {
    private val conversations = ConcurrentHashMap<Long, LeadRequestDraft>()

    fun start(chatId: Long): ConversationResult {
        if (demoMode) {
            conversations.remove(chatId)
            return ConversationResult.reply("Choose an option:", demoKeyboard())
        }

        return startLeadFlow(chatId)
    }

    fun startDemo(chatId: Long): ConversationResult {
        if (!demoMode) {
            return handleText(chatId, TRY_DEMO_BUTTON)
        }

        return startLeadFlow(chatId)
    }

    private fun startLeadFlow(chatId: Long): ConversationResult {
        conversations[chatId] = LeadRequestDraft(state = LeadState.WAITING_NAME)
        return ConversationResult.reply("Enter your name:", removeKeyboard())
    }

    fun cancel(chatId: Long): ConversationResult {
        val removedDraft = conversations.remove(chatId)

        return if (removedDraft == null) {
            ConversationResult.reply("There is no active lead request. Press /start to begin.")
        } else {
            ConversationResult.reply("Lead request was reset. Press /start to begin again.", removeKeyboard())
        }
    }

    fun handleText(chatId: Long, text: String): ConversationResult {
        val draft = conversations[chatId] ?: return if (demoMode) {
            ConversationResult.reply("Please choose an option:", demoKeyboard())
        } else {
            ConversationResult.reply("Press /start to create a lead request.", removeKeyboard())
        }

        val trimmedText = text.trim()
        if (trimmedText.isBlank()) {
            return ConversationResult.reply("Please send a text value.")
        }

        return when (draft.state) {
            LeadState.WAITING_NAME -> {
                conversations[chatId] = draft.copy(name = trimmedText, state = LeadState.WAITING_PHONE)
                ConversationResult.reply("Enter your phone:")
            }

            LeadState.WAITING_PHONE -> {
                if (!phoneValidator.isValid(trimmedText)) {
                    return ConversationResult.reply("Enter a valid phone number, for example +375291234567.")
                }

                conversations[chatId] = draft.copy(phone = trimmedText, state = LeadState.WAITING_MESSAGE)
                ConversationResult.reply("Describe what you need:")
            }

            LeadState.WAITING_MESSAGE -> {
                val request = LeadRequestDto(
                    name = draft.name.orEmpty(),
                    phone = draft.phone.orEmpty(),
                    message = trimmedText,
                    createdAt = LocalDateTime.now(),
                )

                conversations.remove(chatId)
                leadRequestMemoryStorage.save(request)

                if (demoMode) {
                    return ConversationResult.reply(
                        demoAdminPreview(request),
                        removeKeyboard(),
                    )
                }

                ConversationResult.completed(
                    userReply = "Thank you, your request has been sent.",
                    request = request,
                    replyMarkup = removeKeyboard(),
                )
            }

            LeadState.DONE -> {
                conversations.remove(chatId)
                ConversationResult.reply("Press /start to create a new lead request.")
            }
        }
    }

    fun removeKeyboard(): ReplyKeyboardRemove =
        ReplyKeyboardRemove.builder()
            .removeKeyboard(true)
            .build()

    private fun demoKeyboard(): ReplyKeyboardMarkup = keyboard(listOf(TRY_DEMO_BUTTON, ORDER_BUTTON))

    private fun demoAdminPreview(request: LeadRequestDto): String =
        "Admin will receive:\n\n${leadRequestAdminMessageFormatter.format(request)}"

    private fun keyboard(buttons: List<String>): ReplyKeyboardMarkup {
        val rows = buttons.map { button ->
            KeyboardRow().apply {
                add(button)
            }
        }

        return ReplyKeyboardMarkup.builder()
            .keyboard(rows)
            .resizeKeyboard(true)
            .oneTimeKeyboard(true)
            .build()
    }

    private companion object {
        const val TRY_DEMO_BUTTON = "📝 Try demo"
        const val ORDER_BUTTON = "🚀 Order a similar bot"
    }
}

data class ConversationResult(
    val userReply: String,
    val request: LeadRequestDto? = null,
    val replyMarkup: ReplyKeyboard? = null,
) {
    companion object {
        fun reply(text: String, replyMarkup: ReplyKeyboard? = null): ConversationResult =
            ConversationResult(userReply = text, replyMarkup = replyMarkup)

        fun completed(
            userReply: String,
            request: LeadRequestDto,
            replyMarkup: ReplyKeyboard? = null,
        ): ConversationResult = ConversationResult(
            userReply = userReply,
            request = request,
            replyMarkup = replyMarkup,
        )
    }
}

private data class LeadRequestDraft(
    val state: LeadState,
    val name: String? = null,
    val phone: String? = null,
)
