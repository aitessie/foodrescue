package com.example.foodrescue.orderservice.application.ports

import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.PickupToken

interface PickupTokenDBPort {
    fun findByOrderId(orderId: OrderId): PickupToken?

    fun save(pickupToken: PickupToken): PickupToken
}
