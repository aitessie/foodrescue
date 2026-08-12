package com.example.foodrescue.offerservice.application.exception

class PartnerServiceAuthenticationException(
    message: String = "Partner Service rejected offer-service authentication",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
