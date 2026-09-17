package com.example.foodrescue.orderservice.adapter.out.db.mappers

import com.example.foodrescue.orderservice.adapter.out.db.entities.OrderJpaEntity
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.StoreId
import org.springframework.stereotype.Component

@Component
class OrderJpaMapper {
    fun toDomain(entity: OrderJpaEntity): Order =
        Order(
            id = OrderId(entity.id),
            customerId = entity.customerId,
            offerId = OfferId(entity.offerId),
            storeId = StoreId(entity.storeId),
            quantity = entity.quantity,
            unitPrice = entity.unitPrice,
            totalAmount = entity.totalAmount,
            pickupStart = entity.pickupStart,
            pickupEnd = entity.pickupEnd,
            status = entity.status,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )

    fun toJpaEntity(order: Order): OrderJpaEntity =
        OrderJpaEntity(
            id = order.id.value,
            customerId = order.customerId,
            offerId = order.offerId.value,
            storeId = order.storeId.value,
            quantity = order.quantity,
            unitPrice = order.unitPrice,
            totalAmount = order.totalAmount,
            pickupStart = order.pickupStart,
            pickupEnd = order.pickupEnd,
            status = order.status,
            version = order.version,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt,
        )
}
