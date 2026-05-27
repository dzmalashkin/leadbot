package com.ipdim.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LeadBotApplication

fun main(args: Array<String>) {
    runApplication<LeadBotApplication>(*args)
}
