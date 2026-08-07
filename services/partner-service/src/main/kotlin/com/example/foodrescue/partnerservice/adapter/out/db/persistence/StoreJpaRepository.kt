package com.example.foodrescue.partnerservice.adapter.out.db.persistence

import com.example.foodrescue.partnerservice.adapter.out.db.entity.StoreJpaEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface StoreJpaRepository : JpaRepository<StoreJpaEntity, UUID> {
    fun findAllByPartnerId(partnerId: UUID): List<StoreJpaEntity>
}
