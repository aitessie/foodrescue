package com.example.foodrescue.offerservice.configuration

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.net.URI
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "food-rescue.partner-oauth")
data class PartnerOAuthProperties(
    @field:NotBlank val registrationId: String,
    @field:NotNull val tokenUri: URI,
    @field:NotBlank val clientId: String,
    @field:NotBlank val clientSecret: String,
)
