package com.ipdim.app.service

import com.ipdim.app.dto.LeadRequestDto
import org.springframework.stereotype.Service
import java.util.concurrent.CopyOnWriteArrayList

@Service
class LeadRequestMemoryStorage {
    private val requests = CopyOnWriteArrayList<LeadRequestDto>()

    fun save(request: LeadRequestDto): LeadRequestDto {
        requests.add(request)
        return request
    }

    fun findAll(): List<LeadRequestDto> = requests.toList()
}
