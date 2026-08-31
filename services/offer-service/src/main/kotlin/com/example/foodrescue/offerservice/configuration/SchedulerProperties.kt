package com.example.foodrescue.offerservice.configuration

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("food-rescue.scheduler")
data class SchedulerProperties(
    val closeExpiredEnabled: Boolean = true,
    @field:NotBlank val closeExpiredCron: String = "0 */1 * * * *",
    @field:NotBlank val closeExpiredZone: String = "UTC",
    @field:Min(1) val closeExpiredBatchSize: Int = 100,
)
