package com.example.foodrescue.orderservice.application.exceptions

import com.example.foodrescue.orderservice.domain.entities.OrderId

class OrderNotFoundException(orderId: OrderId) :
    RuntimeException("Order not found: ${orderId.value}")
