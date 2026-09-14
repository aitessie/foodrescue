package com.example.foodrescue.orderservice.adapter.out.db.persistence

import com.example.foodrescue.orderservice.adapter.out.db.mappers.OrderJpaMapper
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import org.springframework.stereotype.Component

@Component
class OrderDBAdapterService(
    private val orderJpaRepository: OrderJpaRepository,
    private val orderJpaMapper: OrderJpaMapper,
) : OrderDBPort {
    override fun findById(orderId: OrderId): Order? =
        orderJpaRepository.findById(orderId.value)?.let(orderJpaMapper::toDomain)
}
