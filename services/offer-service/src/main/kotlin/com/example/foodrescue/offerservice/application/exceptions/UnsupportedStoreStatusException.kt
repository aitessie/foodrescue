package com.example.foodrescue.offerservice.application.exceptions

class UnsupportedStoreStatusException(status: String) :
    RuntimeException("Unsupported Store status: $status")
