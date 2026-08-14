package com.example.foodrescue.offerservice.application.exceptions

class UnsupportedPartnerEventTypeException(eventType: String) :
    RuntimeException("Unsupported Partner event type: $eventType")
