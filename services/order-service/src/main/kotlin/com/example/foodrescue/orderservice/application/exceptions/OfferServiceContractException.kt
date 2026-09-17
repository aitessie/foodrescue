package com.example.foodrescue.orderservice.application.exceptions

class OfferServiceContractException(
    message: String = "Offer Service rejected the offer snapshot contract",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
