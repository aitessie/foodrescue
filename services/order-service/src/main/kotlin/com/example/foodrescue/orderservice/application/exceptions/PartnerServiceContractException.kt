package com.example.foodrescue.orderservice.application.exceptions

class PartnerServiceContractException(
    message: String = "Partner Service rejected the access-check contract",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
