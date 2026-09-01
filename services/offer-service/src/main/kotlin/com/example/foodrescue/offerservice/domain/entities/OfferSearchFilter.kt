package com.example.foodrescue.offerservice.domain.entities

import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory

data class OfferSearchFilter(
    val storeId: StoreId?,
    val category: FoodBagCategory?,
    val page: Int,
    val size: Int,
)
