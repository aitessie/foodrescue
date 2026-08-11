package com.example.foodrescue.offerservice.configuration

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "food-rescue.search")
data class OfferSearchProperties(
    @field:Min(1) @field:Max(100) val defaultLimit: Int,
    @field:Min(1) @field:Max(100) val minLimit: Int,
    @field:Min(1) @field:Max(100) val maxLimit: Int,
    @field:Min(1) val maxRadiusMeters: Int,
) {
    @get:AssertTrue(message = "Search limits must satisfy minLimit <= defaultLimit <= maxLimit")
    val limitsAreConsistent: Boolean
        get() = minLimit <= defaultLimit && defaultLimit <= maxLimit
}
