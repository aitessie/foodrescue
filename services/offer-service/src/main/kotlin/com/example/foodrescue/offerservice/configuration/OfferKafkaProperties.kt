package com.example.foodrescue.offerservice.configuration

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "food-rescue.kafka")
data class OfferKafkaProperties(
    @field:NotBlank val partnerEventsTopic: String,
    @field:NotBlank val partnerEventsDltTopic: String,
    @field:NotBlank val offerEventsTopic: String,
)
