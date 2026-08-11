package com.example.foodrescue.offerservice.configuration

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Duration
import org.hibernate.validator.constraints.time.DurationMin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "food-rescue.scheduler")
data class OfferSchedulerProperties(
    val closeExpiredEnabled: Boolean,
    @field:DurationMin(millis = 1) val closeExpiredInitialDelay: Duration,
    @field:DurationMin(millis = 1) val closeExpiredFixedDelay: Duration,
    @field:Min(1) @field:Max(1000) val closeExpiredBatchSize: Int,
)
