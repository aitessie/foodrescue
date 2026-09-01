package com.example.foodrescue.offerservice.adapter.`out`.db.entities

import com.example.foodrescue.offerservice.domain.`enum`.Allergen
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "offers")
class OfferJpaEntity(
    @Id
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
    )
    var id: UUID,
    @Column(
        name = "store_id",
        nullable = false,
        updatable = false,
    )
    var storeId: UUID,
    @Column(
        name = "food_bag_id",
        nullable = false,
        updatable = false,
    )
    var foodBagId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(
        name = "category",
        nullable = false,
        updatable = false,
        length = 64,
    )
    var category: FoodBagCategory,
    @Column(
        name = "unit_price_minor",
        nullable = false,
        updatable = false,
    )
    var unitPriceMinor: Long,
    @Enumerated(EnumType.STRING)
    @Column(
        name = "currency",
        nullable = false,
        updatable = false,
        length = 3,
    )
    var currency: MoneyCurrency,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "offer_allergens",
        joinColumns =
            [
                JoinColumn(
                    name = "offer_id",
                    nullable = false,
                )
            ],
    )
    @Enumerated(EnumType.STRING)
    @Column(
        name = "allergen",
        nullable = false,
        length = 64,
    )
    var allergens: MutableSet<Allergen>,
    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 32,
    )
    var status: OfferStatus,
    @Column(
        name = "total_quantity",
        nullable = false,
    )
    var totalQuantity: Int,
    @Column(
        name = "available_quantity",
        nullable = false,
    )
    var availableQuantity: Int,
    @Column(
        name = "pickup_start",
        nullable = false,
    )
    var pickupStart: Instant,
    @Column(
        name = "pickup_end",
        nullable = false,
    )
    var pickupEnd: Instant,
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
    )
    var createdAt: Instant,
    @Column(
        name = "updated_at",
        nullable = false,
    )
    var updatedAt: Instant,
    @Version
    @Column(
        name = "version",
        nullable = false,
    )
    var version: Long,
)
