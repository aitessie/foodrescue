package com.example.foodrescue.orderservice.adapter.out.db.mappers

import com.example.foodrescue.orderservice.adapter.out.db.entities.PickupTokenJpaEntity
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.PickupToken
import org.springframework.stereotype.Component

@Component
class PickupTokenJpaMapper {
    fun toDomain(entity: PickupTokenJpaEntity): PickupToken =
        PickupToken(
            orderId = OrderId(entity.orderId),
            tokenHash = entity.tokenHash,
            usedAt = entity.usedAt,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )

    fun toJpaEntity(pickupToken: PickupToken): PickupTokenJpaEntity =
        PickupTokenJpaEntity(
            orderId = pickupToken.orderId.value,
            tokenHash = pickupToken.tokenHash,
            usedAt = pickupToken.usedAt,
            version = pickupToken.version,
            createdAt = pickupToken.createdAt,
            updatedAt = pickupToken.updatedAt,
        )
}
