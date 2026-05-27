package com.ipdim.app.service

import com.ipdim.app.dto.LeadRequestDto
import com.ipdim.app.model.LeadState
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class LeadRequestConversationService(
    private val leadRequestMemoryStorage: LeadRequestMemoryStorage,
) {
    private val conversations = ConcurrentHashMap<Long, LeadRequestDraft>()

    fun start(chatId: Long): ConversationResult {
        conversations[chatId] = LeadRequestDraft(state = LeadState.WAITING_NAME)
        return ConversationResult.reply("Введите имя")
    }

    fun cancel(chatId: Long): ConversationResult {
        val removedDraft = conversations.remove(chatId)

        return if (removedDraft == null) {
            ConversationResult.reply("Активной заявки нет. Нажмите /start, чтобы оставить заявку")
        } else {
            ConversationResult.reply("Заявка сброшена. Нажмите /start, чтобы начать заново")
        }
    }

    fun handleText(chatId: Long, text: String): ConversationResult {
        val draft = conversations[chatId] ?: return ConversationResult.reply("Нажмите /start, чтобы оставить заявку")

        return when (draft.state) {
            LeadState.WAITING_NAME -> {
                conversations[chatId] = draft.copy(name = text, state = LeadState.WAITING_PHONE)
                ConversationResult.reply("Введите телефон")
            }

            LeadState.WAITING_PHONE -> {
                conversations[chatId] = draft.copy(phone = text, state = LeadState.WAITING_MESSAGE)
                ConversationResult.reply("Опишите, что вам нужно")
            }

            LeadState.WAITING_MESSAGE -> {
                val request = LeadRequestDto(
                    name = draft.name.orEmpty(),
                    phone = draft.phone.orEmpty(),
                    message = text,
                )

                conversations.remove(chatId)
                leadRequestMemoryStorage.save(request)

                ConversationResult.completed(
                    userReply = "Спасибо, заявка отправлена",
                    request = request,
                )
            }

            LeadState.DONE -> {
                conversations.remove(chatId)
                ConversationResult.reply("Нажмите /start, чтобы оставить новую заявку")
            }
        }
    }
}

data class ConversationResult(
    val userReply: String,
    val request: LeadRequestDto? = null,
) {
    companion object {
        fun reply(text: String): ConversationResult = ConversationResult(userReply = text)

        fun completed(userReply: String, request: LeadRequestDto): ConversationResult =
            ConversationResult(userReply = userReply, request = request)
    }
}

private data class LeadRequestDraft(
    val state: LeadState,
    val name: String? = null,
    val phone: String? = null,
)
