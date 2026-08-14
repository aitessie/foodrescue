package com.example.foodrescue.offerservice.application.exceptions

class InvalidCursorException(
    reason: String,
    cause: Throwable? = null,
) :
    RuntimeException(
        "Invalid cursor: $reason",
        cause,
    )
