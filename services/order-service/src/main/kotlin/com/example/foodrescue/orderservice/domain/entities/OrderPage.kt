package com.example.foodrescue.orderservice.domain.entities

class OrderPage(
    content: List<Order>,
    val totalElements: Long,
    val totalPages: Int,
    val pageNumber: Int,
    val pageSize: Int,
) {
    private val contentValues = content.toList()

    val content: List<Order>
        get() = contentValues.toList()
}
