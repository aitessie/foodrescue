package com.example.foodrescue.offerservice.adapter.`out`.db.persistence

import com.example.foodrescue.offerservice.adapter.`out`.db.entities.OfferJpaEntity
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OfferJpaRepository : JpaRepository<OfferJpaEntity, UUID> {
    @Query(
        """
        select offer
        from OfferJpaEntity offer
        where offer.status in :statuses
          and offer.pickupEnd <= :expiredAt
        order by offer.pickupEnd asc, offer.id asc
        """
    )
    fun findExpiredBatch(
        @Param("statuses") statuses: Set<OfferStatus>,
        @Param("expiredAt") expiredAt: Instant,
        pageable: Pageable,
    ): List<OfferJpaEntity>
}
