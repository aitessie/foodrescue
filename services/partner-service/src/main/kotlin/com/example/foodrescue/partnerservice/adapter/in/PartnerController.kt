package com.example.foodrescue.partnerservice.adapter.`in`

import com.example.foodrescue.partnerservice.adapter.`in`.dtos.PartnerDto
import com.example.foodrescue.partnerservice.adapter.`in`.mapper.PartnerRestMapper
import com.example.foodrescue.partnerservice.application.usecases.GetPartnerUseCase
import com.example.foodrescue.partnerservice.application.usecases.CreateOrUpdatePartnerUseCase
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/partners")
class PartnerController(
    private val getPartnerUseCase: GetPartnerUseCase,
    private val createOrUpdatePartnerUseCase: CreateOrUpdatePartnerUseCase,
    private val partnerRestMapper: PartnerRestMapper,
) {

    @GetMapping("/{partnerId}")
    fun getPartner(
        @PathVariable partnerId: UUID,
    ): PartnerDto {
        val partner = getPartnerUseCase.getPartner(PartnerId(partnerId))

        return partnerRestMapper.toDto(partner)
    }

    @PutMapping("/{partnerId}")
    fun createOrUpdatePartner(
        @PathVariable partnerId: UUID,
        @Valid @RequestBody partnerDto: PartnerDto,
    ): PartnerDto {
        val source = partnerRestMapper.toDomain(
            dto = partnerDto,
            partnerId = PartnerId(partnerId),
        )

        val savedPartner = createOrUpdatePartnerUseCase.createOrUpdatePartner(source)

        return partnerRestMapper.toDto(savedPartner)
    }
}
