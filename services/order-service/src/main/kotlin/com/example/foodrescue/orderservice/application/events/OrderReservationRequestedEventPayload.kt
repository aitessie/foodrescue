package com.example.foodrescue.orderservice.application.events

import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.OrderId

class OrderReservationRequestedEventPayload(
    val orderId: OrderId,
    val offerId: OfferId,
    val customerId: String,
    val quantity: Int,
) : ApplicationEventPayload
