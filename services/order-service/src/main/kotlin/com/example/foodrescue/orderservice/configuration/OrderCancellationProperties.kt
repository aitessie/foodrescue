package com.example.foodrescue.orderservice.configuration

import java.time.Duration
import org.hibernate.validator.constraints.time.DurationMin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "food-rescue.order-cancellation")
data class OrderCancellationProperties(
    @field:DurationMin(millis = 0) val customerDeadlineBeforePickupStart: Duration
)
