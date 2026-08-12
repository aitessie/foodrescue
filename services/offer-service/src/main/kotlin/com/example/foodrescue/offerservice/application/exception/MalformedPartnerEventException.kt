package com.example.foodrescue.offerservice.application.exception

class MalformedPartnerEventException(
    message: String = "Malformed Partner event",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
