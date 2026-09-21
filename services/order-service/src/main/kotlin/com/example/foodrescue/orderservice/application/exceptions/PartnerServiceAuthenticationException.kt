package com.example.foodrescue.orderservice.application.exceptions

class PartnerServiceAuthenticationException(
    message: String = "Partner Service rejected order-service authentication",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
