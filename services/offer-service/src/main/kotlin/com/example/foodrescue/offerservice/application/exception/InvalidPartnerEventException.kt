package com.example.foodrescue.offerservice.application.exception

import java.util.UUID

class InvalidPartnerEventException(
    eventId: UUID,
    message: String,
) : RuntimeException("Partner event '$eventId' is invalid: $message")
