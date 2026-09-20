package com.example.foodrescue.orderservice.application.ports

interface PickupTokenHashPort {
    fun hash(token: String): String
}
