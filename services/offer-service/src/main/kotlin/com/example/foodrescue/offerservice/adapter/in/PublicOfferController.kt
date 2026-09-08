package com.example.foodrescue.offerservice.adapter.`in`

import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferSearchItemDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferSearchPageDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferSearchQuery
import com.example.foodrescue.offerservice.adapter.`in`.mappers.OfferRestMapper
import com.example.foodrescue.offerservice.application.usecases.GetPublicOfferUseCase
import com.example.foodrescue.offerservice.application.usecases.SearchOffersUseCase
import com.example.foodrescue.offerservice.domain.entities.OfferId
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/offers")
class PublicOfferController(
    private val getPublicOfferUseCase: GetPublicOfferUseCase,
    private val searchOffersUseCase: SearchOffersUseCase,
    private val offerRestMapper: OfferRestMapper,
) {
    @GetMapping("/{offerId}")
    fun getPublicOffer(@PathVariable offerId: UUID): OfferSearchItemDto {
        val offer = getPublicOfferUseCase.execute(OfferId(offerId))

        return offerRestMapper.toSearchItemDto(offer)
    }

    @GetMapping
    fun searchOffers(@Valid @ModelAttribute query: OfferSearchQuery): OfferSearchPageDto {
        val filter = offerRestMapper.toSearchFilter(query)
        val page = searchOffersUseCase.execute(filter)

        return offerRestMapper.toSearchPageDto(page)
    }
}
