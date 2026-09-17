package com.example.foodrescue.orderservice.application.exceptions

class OfferServiceUnavailableException(cause: Throwable? = null) :
    RuntimeException("Offer Service is unavailable", cause)
