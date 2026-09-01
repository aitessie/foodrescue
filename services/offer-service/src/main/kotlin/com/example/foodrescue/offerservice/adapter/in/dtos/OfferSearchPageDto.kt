package com.example.foodrescue.offerservice.adapter.`in`.dtos

data class OfferSearchPageDto(
    val items: List<OfferSearchItemDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
