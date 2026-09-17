package com.example.foodrescue.orderservice.application.exceptions

class OfferServiceAuthenticationException(
    message: String = "Offer Service rejected order-service authentication",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
