package com.example.foodrescue.orderservice.adapter.out.db.persistence

import com.example.foodrescue.orderservice.adapter.out.db.mappers.OrderJpaMapper
import com.example.foodrescue.orderservice.application.ports.OrderDBPort
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.OrderPage
import com.example.foodrescue.orderservice.domain.enum.OrderStatus
import java.time.Instant
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class OrderRepository(
    private val orderJpaRepository: OrderJpaRepository,
    private val orderJpaMapper: OrderJpaMapper,
) : OrderDBPort {
    override fun findById(orderId: OrderId): Order? =
        orderJpaRepository.findById(orderId.value)?.let(orderJpaMapper::toDomain)

    override fun findByCustomerId(
        customerId: String,
        page: Int,
        size: Int,
    ): OrderPage {
        val pageable =
            PageRequest.of(
                page,
                size,
                Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id"),
                ),
            )
        val result = orderJpaRepository.findAllByCustomerId(customerId, pageable)

        return OrderPage(
            content = result.content.map(orderJpaMapper::toDomain),
            pageNumber = result.number,
            pageSize = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    override fun findNoShowCandidates(
        pickupEndedAt: Instant,
        batchSize: Int,
    ): List<Order> =
        orderJpaRepository
            .findAllByStatusAndPickupEndLessThanEqual(
                status = OrderStatus.RESERVED,
                pickupEnd = pickupEndedAt,
                pageable =
                    PageRequest.of(
                        0,
                        batchSize,
                        Sort.by(
                            Sort.Order.asc("pickupEnd"),
                            Sort.Order.asc("id"),
                        ),
                    ),
            )
            .map(orderJpaMapper::toDomain)

    override fun save(order: Order): Order =
        orderJpaRepository.save(orderJpaMapper.toJpaEntity(order)).let(orderJpaMapper::toDomain)
}
