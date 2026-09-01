package com.example.foodrescue.offerservice.adapter.out.db.persistence

import com.example.foodrescue.offerservice.adapter.out.db.entities.FoodBagJpaEntity
import com.example.foodrescue.offerservice.adapter.out.db.entities.OfferJpaEntity
import com.example.foodrescue.offerservice.adapter.out.db.entities.StoreSnapshotJpaEntity

interface PublicOfferJpaProjection {
    val offer: OfferJpaEntity

    val foodBag: FoodBagJpaEntity

    val storeSnapshot: StoreSnapshotJpaEntity
}
