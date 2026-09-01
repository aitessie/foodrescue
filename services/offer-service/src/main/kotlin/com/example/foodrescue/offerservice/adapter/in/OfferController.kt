package com.example.foodrescue.offerservice.adapter.`in`

import com.example.foodrescue.offerservice.adapter.`in`.dtos.ChangeOfferQuantityDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.CreateOrUpdateOfferDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferStatusDto
import com.example.foodrescue.offerservice.adapter.`in`.mappers.OfferRestMapper
import com.example.foodrescue.offerservice.application.usecases.ChangeOfferQuantityUseCase
import com.example.foodrescue.offerservice.application.usecases.ChangeOfferStatusUseCase
import com.example.foodrescue.offerservice.application.usecases.CreateOrUpdateOfferUseCase
import com.example.foodrescue.offerservice.application.usecases.GetManagedOfferUseCase
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/partners/{partnerId}/stores/{storeId}/offers")
class OfferController(
    private val createOrUpdateOfferUseCase: CreateOrUpdateOfferUseCase,
    private val getManagedOfferUseCase: GetManagedOfferUseCase,
    private val changeOfferStatusUseCase: ChangeOfferStatusUseCase,
    private val changeOfferQuantityUseCase: ChangeOfferQuantityUseCase,
    private val offerRestMapper: OfferRestMapper,
) {
    @GetMapping("/{offerId}")
    fun getManagedOffer(
        @PathVariable partnerId: UUID,
        @PathVariable storeId: UUID,
        @PathVariable offerId: UUID,
    ): OfferDto {
        val offer =
            getManagedOfferUseCase.execute(
                PartnerId(partnerId),
                StoreId(storeId),
                OfferId(offerId),
            )

        return offerRestMapper.toDto(offer)
    }

    @PutMapping("/{offerId}")
    fun createOrUpdateOffer(
        @PathVariable partnerId: UUID,
        @PathVariable storeId: UUID,
        @PathVariable offerId: UUID,
        @Valid @RequestBody dto: CreateOrUpdateOfferDto,
    ): OfferDto {
        val offer =
            createOrUpdateOfferUseCase.execute(
                PartnerId(partnerId),
                StoreId(storeId),
                OfferId(offerId),
                offerRestMapper.toFoodBagId(dto),
                dto.totalQuantity,
                offerRestMapper.toPickupWindow(dto),
                dto.version,
            )

        return offerRestMapper.toDto(offer)
    }

    @GetMapping("/{offerId}/status")
    fun getManagedOfferStatus(
        @PathVariable partnerId: UUID,
        @PathVariable storeId: UUID,
        @PathVariable offerId: UUID,
    ): OfferStatusDto {
        val offer =
            getManagedOfferUseCase.execute(
                PartnerId(partnerId),
                StoreId(storeId),
                OfferId(offerId),
            )

        return offerRestMapper.toStatusDto(offer)
    }

    @PutMapping("/{offerId}/status")
    fun changeOfferStatus(
        @PathVariable partnerId: UUID,
        @PathVariable storeId: UUID,
        @PathVariable offerId: UUID,
        @Valid @RequestBody dto: OfferStatusDto,
    ): OfferStatusDto {
        val offer =
            changeOfferStatusUseCase.execute(
                PartnerId(partnerId),
                StoreId(storeId),
                OfferId(offerId),
                dto.status,
                dto.version,
            )

        return offerRestMapper.toStatusDto(offer)
    }

    @PutMapping("/{offerId}/quantity")
    fun changeOfferQuantity(
        @PathVariable partnerId: UUID,
        @PathVariable storeId: UUID,
        @PathVariable offerId: UUID,
        @Valid @RequestBody dto: ChangeOfferQuantityDto,
    ): OfferDto {
        val offer =
            changeOfferQuantityUseCase.execute(
                PartnerId(partnerId),
                StoreId(storeId),
                OfferId(offerId),
                dto.totalQuantity,
                dto.version,
            )

        return offerRestMapper.toDto(offer)
    }
}
