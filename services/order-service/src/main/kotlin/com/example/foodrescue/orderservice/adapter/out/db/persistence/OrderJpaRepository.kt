package com.example.foodrescue.orderservice.adapter.out.db.persistence

import com.example.foodrescue.orderservice.adapter.out.db.entities.OrderJpaEntity
import java.util.UUID
import org.springframework.data.repository.Repository

interface OrderJpaRepository : Repository<OrderJpaEntity, UUID> {
    fun findById(id: UUID): OrderJpaEntity?

    fun save(order: OrderJpaEntity): OrderJpaEntity
}
