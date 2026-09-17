package com.example.foodrescue.orderservice.application.ports

import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId

interface OrderDBPort {
    fun findById(orderId: OrderId): Order?

    fun save(order: Order): Order
}
