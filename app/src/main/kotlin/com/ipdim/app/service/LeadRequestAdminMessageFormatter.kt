package com.ipdim.app.service

import com.ipdim.app.dto.LeadRequestDto
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class LeadRequestAdminMessageFormatter {
    fun format(request: LeadRequestDto): String = """
        🆕 Новая заявка

        👤 Имя: ${request.name}
        📞 Телефон: ${request.phone}
        💬 Сообщение: ${request.message}

        🕒 Время: ${request.createdAt.format(formatter)}
    """.trimIndent()

    private companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
