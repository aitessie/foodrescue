package com.example.foodrescue.orderservice.configuration

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "food-rescue.scheduler")
data class OrderSchedulerProperties(
    @field:Min(1) val noShowBatchSize: Int,
    val noShowPaymentAction: NoShowPaymentAction,
)
