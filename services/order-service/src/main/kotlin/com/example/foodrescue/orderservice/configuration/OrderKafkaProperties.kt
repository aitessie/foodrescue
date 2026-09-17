package com.example.foodrescue.orderservice.configuration

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "food-rescue.kafka")
data class OrderKafkaProperties(
    @field:NotBlank val orderCommandsTopic: String,
)
