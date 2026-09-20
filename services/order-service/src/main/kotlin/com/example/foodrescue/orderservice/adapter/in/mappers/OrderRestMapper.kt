package com.example.foodrescue.orderservice.adapter.`in`.mappers

import com.example.foodrescue.orderservice.adapter.`in`.dtos.OrderDto
import com.example.foodrescue.orderservice.adapter.`in`.dtos.OrderPageDto
import com.example.foodrescue.orderservice.adapter.`in`.dtos.PickupTokenDto
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.Order
import com.example.foodrescue.orderservice.domain.entities.OrderId
import com.example.foodrescue.orderservice.domain.entities.OrderPage
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class OrderRestMapper {
    fun toOrderId(orderId: UUID): OrderId = OrderId(orderId)

    fun toOfferId(offerId: UUID): OfferId = OfferId(offerId)

    fun toDto(order: Order): OrderDto =
        OrderDto(
            orderId = order.id.value,
            offerId = order.offerId.value,
            storeId = order.storeId.value,
            quantity = order.quantity,
            unitPrice = order.unitPrice,
            totalAmount = order.totalAmount,
            pickupStart = order.pickupStart,
            pickupEnd = order.pickupEnd,
            status = order.status,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt,
        )

    fun toPickupTokenDto(token: String): PickupTokenDto = PickupTokenDto(token)

    fun toPageDto(page: OrderPage): OrderPageDto =
        OrderPageDto(
            items = page.content.map(::toDto),
            page = page.pageNumber,
            size = page.pageSize,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
}
