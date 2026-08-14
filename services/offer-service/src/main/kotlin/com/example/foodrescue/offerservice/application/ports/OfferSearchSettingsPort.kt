package com.example.foodrescue.offerservice.application.ports

interface OfferSearchSettingsPort {
    val defaultLimit: Int
    val minLimit: Int
    val maxLimit: Int
    val maxRadiusMeters: Int
}
