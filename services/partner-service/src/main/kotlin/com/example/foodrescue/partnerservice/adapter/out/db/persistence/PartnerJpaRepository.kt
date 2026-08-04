package com.example.foodrescue.partnerservice.adapter.out.db.persistence

import com.example.foodrescue.partnerservice.adapter.out.db.entity.PartnerJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PartnerJpaRepository : JpaRepository<PartnerJpaEntity, UUID> {
    fun findAllByManagerId(partnerId: String): List<PartnerJpaEntity>
}
