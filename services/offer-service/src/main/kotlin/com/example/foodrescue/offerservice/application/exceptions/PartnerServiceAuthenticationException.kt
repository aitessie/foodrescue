package com.example.foodrescue.offerservice.application.exceptions

class PartnerServiceAuthenticationException(
    message: String = "Partner Service rejected offer-service authentication",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
