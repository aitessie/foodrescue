package com.example.foodrescue.offerservice.domain.entities

class OfferSearchPage(
    items: List<OfferSearchItem>,
    val nextCursor: String?,
) {
    private val itemValues: List<OfferSearchItem> = items.toList()

    val items: List<OfferSearchItem>
        get() = itemValues.toList()
}
