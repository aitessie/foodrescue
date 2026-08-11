package com.example.foodrescue.offerservice.configuration

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class TimeConfiguration {
    @Bean fun clock(): Clock = Clock.systemUTC()
}
