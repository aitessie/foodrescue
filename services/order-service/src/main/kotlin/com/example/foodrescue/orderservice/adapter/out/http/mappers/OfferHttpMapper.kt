package com.example.foodrescue.orderservice.adapter.out.http.mappers

import com.example.foodrescue.orderservice.adapter.out.http.dtos.OfferResponseDto
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.OfferSnapshot
import com.example.foodrescue.orderservice.domain.entities.StoreId
import org.springframework.stereotype.Component

@Component
class OfferHttpMapper {
    fun toSnapshot(dto: OfferResponseDto): OfferSnapshot =
        OfferSnapshot(
            offerId = OfferId(dto.offerId),
            storeId = StoreId(dto.storeId),
            status = dto.status,
            unitPrice = dto.unitPrice,
            availableQuantity = dto.availableQuantity,
            pickupStart = dto.pickupStart,
            pickupEnd = dto.pickupEnd,
        )
}
