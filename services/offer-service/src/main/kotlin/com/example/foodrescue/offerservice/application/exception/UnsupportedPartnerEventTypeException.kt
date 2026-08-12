package com.example.foodrescue.offerservice.application.exception

class UnsupportedPartnerEventTypeException(eventType: String) :
    RuntimeException("Unsupported Partner event type: $eventType")
