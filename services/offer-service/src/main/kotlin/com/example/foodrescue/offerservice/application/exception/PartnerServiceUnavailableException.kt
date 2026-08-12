package com.example.foodrescue.offerservice.application.exception

class PartnerServiceUnavailableException(
    message: String = "Partner Service is unavailable",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
