package com.example.foodrescue.orderservice.adapter.out.db.persistence

import com.example.foodrescue.orderservice.adapter.out.db.mappers.PickupTokenJpaMapper
import com.example.foodrescue.orderservice.application.ports.PickupTokenDBPort
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.PickupToken
import org.springframework.stereotype.Component

@Component
class PickupTokenRepository(
    private val pickupTokenJpaRepository: PickupTokenJpaRepository,
    private val pickupTokenJpaMapper: PickupTokenJpaMapper,
) : PickupTokenDBPort {
    override fun findByOrderId(orderId: OrderId): PickupToken? =
        pickupTokenJpaRepository
            .findById(orderId.value)
            .orElse(null)
            ?.let(pickupTokenJpaMapper::toDomain)

    override fun findByTokenHash(tokenHash: String): PickupToken? =
        pickupTokenJpaRepository.findByTokenHash(tokenHash)?.let(pickupTokenJpaMapper::toDomain)

    override fun save(pickupToken: PickupToken): PickupToken =
        pickupTokenJpaRepository
            .saveAndFlush(pickupTokenJpaMapper.toJpaEntity(pickupToken))
            .let(pickupTokenJpaMapper::toDomain)
}
