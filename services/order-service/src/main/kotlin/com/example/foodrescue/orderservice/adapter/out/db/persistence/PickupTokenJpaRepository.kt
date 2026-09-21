package com.example.foodrescue.orderservice.adapter.out.db.persistence

import com.example.foodrescue.orderservice.adapter.out.db.entities.PickupTokenJpaEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface PickupTokenJpaRepository : JpaRepository<PickupTokenJpaEntity, UUID> {
    fun findByTokenHash(tokenHash: String): PickupTokenJpaEntity?
}
