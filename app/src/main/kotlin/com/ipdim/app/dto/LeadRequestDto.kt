package com.ipdim.app.dto

import java.time.LocalDateTime

data class LeadRequestDto(
    val name: String,
    val phone: String,
    val message: String,
    val createdAt: LocalDateTime,
)
