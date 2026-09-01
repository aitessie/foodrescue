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
            offer,
            foodBag,
            storeSnapshot
        FROM OfferJpaEntity offer
        JOIN FoodBagJpaEntity foodBag
            ON foodBag.id = offer.foodBagId
        JOIN StoreSnapshotJpaEntity storeSnapshot
            ON storeSnapshot.storeId = offer.storeId
        WHERE offer.id = :offerId
          AND offer.status = :offerStatus
          AND offer.availableQuantity > 0
          AND offer.pickupEnd > :visibleAt
          AND storeSnapshot.partnerStatus = :partnerStatus
          AND storeSnapshot.storeStatus = :storeStatus
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
                offer,
                foodBag,
                storeSnapshot
            FROM OfferJpaEntity offer
            JOIN FoodBagJpaEntity foodBag
                ON foodBag.id = offer.foodBagId
            JOIN StoreSnapshotJpaEntity storeSnapshot
                ON storeSnapshot.storeId = offer.storeId
            WHERE offer.status = :offerStatus
              AND offer.availableQuantity > 0
              AND offer.pickupEnd > :visibleAt
              AND storeSnapshot.partnerStatus = :partnerStatus
              AND storeSnapshot.storeStatus = :storeStatus
              AND (:storeId IS NULL OR offer.storeId = :storeId)
              AND (:category IS NULL OR offer.category = :category)
            ORDER BY offer.pickupEnd ASC, offer.id ASC
            """,
        countQuery =
            """
            SELECT COUNT(offer)
            FROM OfferJpaEntity offer
            JOIN FoodBagJpaEntity foodBag
                ON foodBag.id = offer.foodBagId
            JOIN StoreSnapshotJpaEntity storeSnapshot
                ON storeSnapshot.storeId = offer.storeId
            WHERE offer.status = :offerStatus
              AND offer.availableQuantity > 0
              AND offer.pickupEnd > :visibleAt
              AND storeSnapshot.partnerStatus = :partnerStatus
              AND storeSnapshot.storeStatus = :storeStatus
              AND (:storeId IS NULL OR offer.storeId = :storeId)
              AND (:category IS NULL OR offer.category = :category)
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
