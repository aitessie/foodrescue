package com.example.foodrescue.partnerservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling @SpringBootApplication class PartnerServiceApplication

fun main(args: Array<String>) {
    runApplication<PartnerServiceApplication>(*args)
}
