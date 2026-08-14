package com.example.foodrescue.offerservice.application.exceptions

class PartnerServiceUnavailableException(
    message: String = "Partner Service is unavailable",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
