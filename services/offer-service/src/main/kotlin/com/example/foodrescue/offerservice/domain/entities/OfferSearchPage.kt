package com.example.foodrescue.offerservice.domain.entities

class OfferSearchPage(
    content: List<OfferSearchItem>,
    val totalElements: Long,
    val totalPages: Int,
    val pageNumber: Int,
    val pageSize: Int,
) {
    private val contentValues: List<OfferSearchItem> = content.toList()

    val content: List<OfferSearchItem>
        get() = contentValues.toList()
}
