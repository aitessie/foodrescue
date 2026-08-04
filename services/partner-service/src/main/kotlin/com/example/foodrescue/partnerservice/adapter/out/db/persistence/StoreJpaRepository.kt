package com.example.foodrescue.partnerservice.adapter.out.db.persistence

import com.example.foodrescue.partnerservice.adapter.out.db.entity.StoreJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StoreJpaRepository : JpaRepository<StoreJpaEntity, UUID> {
    fun findAllByPartnerId(partnerId: UUID): List<StoreJpaEntity>
}
