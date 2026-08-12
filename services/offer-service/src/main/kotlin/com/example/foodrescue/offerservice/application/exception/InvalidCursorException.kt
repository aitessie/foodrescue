package com.example.foodrescue.offerservice.application.exception

class InvalidCursorException(
    reason: String,
    cause: Throwable? = null,
) :
    RuntimeException(
        "Invalid cursor: $reason",
        cause,
    )
