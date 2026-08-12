package com.example.foodrescue.offerservice.application.exception

class PartnerServiceContractException(
    message: String = "Partner Service rejected the access-check contract",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
