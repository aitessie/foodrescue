package com.example.foodrescue.offerservice.adapter.`in`.mappers

import com.example.foodrescue.offerservice.adapter.`in`.dtos.CreateOrUpdateOfferDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferReservationDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferSearchItemDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferSearchPageDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferSearchQuery
import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferStatusDto
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.OfferSearchFilter
import com.example.foodrescue.offerservice.domain.entities.OfferSearchItem
import com.example.foodrescue.offerservice.domain.entities.OfferSearchPage
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.StoreId
import org.springframework.stereotype.Component

@Component
class OfferRestMapper {
    fun toFoodBagId(dto: CreateOrUpdateOfferDto): FoodBagId = FoodBagId(dto.foodBagId)

    fun toPickupWindow(dto: CreateOrUpdateOfferDto): PickupWindow =
        PickupWindow(
            start = dto.pickupStart,
            end = dto.pickupEnd,
        )

    fun toDto(offer: Offer): OfferDto =
        OfferDto(
            foodBagId = offer.foodBagId.value,
            totalQuantity = offer.totalQuantity,
            availableQuantity = offer.availableQuantity,
            pickupStart = offer.pickupWindow.start,
            pickupEnd = offer.pickupWindow.end,
            version = offer.version,
        )

    fun toStatusDto(offer: Offer): OfferStatusDto =
        OfferStatusDto(
            status = offer.status,
            version = offer.version,
        )

    fun toReservationDto(reservation: OfferReservation): OfferReservationDto =
        OfferReservationDto(
            quantity = reservation.quantity,
            status = reservation.status,
            createdAt = reservation.createdAt,
            updatedAt = reservation.updatedAt,
            version = reservation.version,
        )

    fun toSearchFilter(query: OfferSearchQuery): OfferSearchFilter =
        OfferSearchFilter(
            storeId = query.storeId?.let(::StoreId),
            category = query.category,
            page = query.page,
            size = query.size,
        )

    fun toSearchPageDto(page: OfferSearchPage): OfferSearchPageDto =
        OfferSearchPageDto(
            items = page.content.map(::toSearchItemDto),
            page = page.pageNumber,
            size = page.pageSize,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )

    fun toSearchItemDto(item: OfferSearchItem): OfferSearchItemDto =
        OfferSearchItemDto(
            offerId = item.offerId.value,
            storeId = item.storeId.value,
            foodBagId = item.foodBagId.value,
            foodBagName = item.foodBagName,
            foodBagDescription = item.foodBagDescription,
            category = item.category,
            originalPriceMinor = item.originalPrice.amountMinor,
            unitPriceMinor = item.unitPrice.amountMinor,
            currency = item.unitPrice.currency,
            allergens = item.allergens,
            availableQuantity = item.availableQuantity,
            pickupStart = item.pickupWindow.start,
            pickupEnd = item.pickupWindow.end,
            storeName = item.storeName,
            storeAddress = item.storeAddress,
            storeTimeZone = item.storeTimeZone,
        )
}
