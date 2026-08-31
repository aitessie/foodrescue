package com.example.foodrescue.offerservice.configuration

import jakarta.validation.constraints.NotBlank
import java.net.URI
import java.time.Duration
import org.hibernate.validator.constraints.time.DurationMin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "food-rescue.partner-http")
data class PartnerHttpProperties(
    val baseUrl: URI,
    @field:DurationMin(millis = 1) val timeout: Duration,
    @field:NotBlank val oauth2RegistrationId: String,
)
