package com.example.foodrescue.partnerservice.adapter.out.db.persistence

import com.example.foodrescue.partnerservice.adapter.out.db.entity.PartnerJpaEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface PartnerJpaRepository : JpaRepository<PartnerJpaEntity, UUID> {
    fun findAllByManagerId(partnerId: String): List<PartnerJpaEntity>
}
