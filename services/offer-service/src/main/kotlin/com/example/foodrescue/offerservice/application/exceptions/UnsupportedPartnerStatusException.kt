package com.example.foodrescue.offerservice.application.exceptions

class UnsupportedPartnerStatusException(status: String) :
    RuntimeException("Unsupported Partner status: $status")
