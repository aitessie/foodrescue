package com.example.foodrescue.offerservice.adapter.out.db.persistence

import com.example.foodrescue.offerservice.adapter.out.db.entities.OfferJpaEntity
import com.example.foodrescue.offerservice.domain.enum.FoodBagCategory
import com.example.foodrescue.offerservice.domain.enum.OfferStatus
import com.example.foodrescue.offerservice.domain.enum.PartnerStatus
import com.example.foodrescue.offerservice.domain.enum.StoreStatus
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

interface PublicOfferJpaRepository : Repository<OfferJpaEntity, UUID> {
    @Query(
        """
        SELECT
            o AS offer,
            fb AS foodBag,
            ss AS storeSnapshot
        FROM OfferJpaEntity o
        JOIN FoodBagJpaEntity fb
            ON fb.id = o.foodBagId
        JOIN StoreSnapshotJpaEntity ss
            ON ss.storeId = o.storeId
        WHERE o.id = :offerId
          AND o.status = :offerStatus
          AND o.availableQuantity > 0
          AND o.pickupEnd > :visibleAt
          AND ss.partnerStatus = :partnerStatus
          AND ss.storeStatus = :storeStatus
        """
    )
    fun findVisibleOfferById(
        @Param("offerId") offerId: UUID,
        @Param("visibleAt") visibleAt: Instant,
        @Param("offerStatus") offerStatus: OfferStatus,
        @Param("partnerStatus") partnerStatus: PartnerStatus,
        @Param("storeStatus") storeStatus: StoreStatus,
    ): PublicOfferJpaProjection?

    @Query(
        value =
            """
        SELECT
            o AS offer,
            fb AS foodBag,
            ss AS storeSnapshot
        FROM OfferJpaEntity o
        JOIN FoodBagJpaEntity fb
            ON fb.id = o.foodBagId
        JOIN StoreSnapshotJpaEntity ss
            ON ss.storeId = o.storeId
        WHERE o.status = :offerStatus
          AND o.availableQuantity > 0
          AND o.pickupEnd > :visibleAt
          AND ss.partnerStatus = :partnerStatus
          AND ss.storeStatus = :storeStatus
          AND (:storeId IS NULL OR o.storeId = :storeId)
          AND (:category IS NULL OR o.category = :category)
        ORDER BY o.pickupEnd ASC, o.id ASC
        """,
        countQuery =
            """
        SELECT COUNT(o)
        FROM OfferJpaEntity o
        JOIN FoodBagJpaEntity fb
            ON fb.id = o.foodBagId
        JOIN StoreSnapshotJpaEntity ss
            ON ss.storeId = o.storeId
        WHERE o.status = :offerStatus
          AND o.availableQuantity > 0
          AND o.pickupEnd > :visibleAt
          AND ss.partnerStatus = :partnerStatus
          AND ss.storeStatus = :storeStatus
          AND (:storeId IS NULL OR o.storeId = :storeId)
          AND (:category IS NULL OR o.category = :category)
        """,
    )
    fun findVisibleOffers(
        @Param("storeId") storeId: UUID?,
        @Param("category") category: FoodBagCategory?,
        @Param("visibleAt") visibleAt: Instant,
        @Param("offerStatus") offerStatus: OfferStatus,
        @Param("partnerStatus") partnerStatus: PartnerStatus,
        @Param("storeStatus") storeStatus: StoreStatus,
        pageable: Pageable,
    ): Page<PublicOfferJpaProjection>
}
