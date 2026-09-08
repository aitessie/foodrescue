package com.example.foodrescue.partnerservice.adapter.`in`

import com.example.foodrescue.partnerservice.adapter.`in`.dtos.PartnerStoreAccessRequestDto
import com.example.foodrescue.partnerservice.adapter.`in`.dtos.PartnerStoreAccessResponseDto
import com.example.foodrescue.partnerservice.adapter.`in`.mappers.PartnerStoreAccessRestMapper
import com.example.foodrescue.partnerservice.application.usecases.CheckPartnerStoreAccessUseCase
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.StoreId
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/api/v1/partner-store-access")
class PartnerStoreAccessController(
    private val checkPartnerStoreAccessUseCase: CheckPartnerStoreAccessUseCase,
    private val partnerStoreAccessRestMapper: PartnerStoreAccessRestMapper,
) {
    @PostMapping("/check")
    fun checkAccess(
        @Valid @RequestBody request: PartnerStoreAccessRequestDto
    ): PartnerStoreAccessResponseDto {
        val snapshot =
            checkPartnerStoreAccessUseCase.execute(
                partnerId = PartnerId(request.partnerId),
                storeId = StoreId(request.storeId),
                userId = request.userId,
            )

        return partnerStoreAccessRestMapper.toDto(snapshot)
    }
}
