package com.example.foodrescue.orderservice.adapter.out.db.persistence

import com.example.foodrescue.orderservice.adapter.out.db.entities.OrderJpaEntity
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.Repository

interface OrderJpaRepository : Repository<OrderJpaEntity, UUID> {
    fun findById(id: UUID): OrderJpaEntity?

    fun findAllByCustomerId(
        customerId: String,
        pageable: Pageable,
    ): Page<OrderJpaEntity>

    fun save(order: OrderJpaEntity): OrderJpaEntity
}
