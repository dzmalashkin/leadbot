package com.ipdim.app.service

import org.springframework.stereotype.Component

@Component
class PhoneValidator {
    private val phoneRegex = Regex("""^\+?[0-9\s\-()]{7,20}$""")

    fun isValid(phone: String): Boolean {
        val digitsCount = phone.count { it.isDigit() }
        return phone.matches(phoneRegex) && digitsCount in MIN_DIGITS..MAX_DIGITS
    }

    private companion object {
        const val MIN_DIGITS = 7
        const val MAX_DIGITS = 15
    }
}
