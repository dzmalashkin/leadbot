package com.ipdim.app.service

import com.ipdim.app.dto.LeadRequestDto
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class LeadRequestAdminMessageFormatter {
    fun format(request: LeadRequestDto): String = """
        🆕 New lead request

        👤 Name: ${request.name}
        📞 Phone: ${request.phone}
        💬 Message: ${request.message}

        🕒 Created: ${request.createdAt.format(formatter)}
    """.trimIndent()

    private companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
