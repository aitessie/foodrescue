package com.example.foodrescue.offerservice.application.exceptions

class MalformedPartnerEventException(
    message: String = "Malformed Partner event",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
