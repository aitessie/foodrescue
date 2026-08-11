package com.example.foodrescue.offerservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication @ConfigurationPropertiesScan @EnableScheduling class OfferServiceApplication

fun main(args: Array<String>) {
    runApplication<OfferServiceApplication>(*args)
}
