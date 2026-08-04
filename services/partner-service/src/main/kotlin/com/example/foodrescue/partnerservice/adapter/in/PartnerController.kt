package com.example.foodrescue.partnerservice.adapter.`in`

import com.example.foodrescue.partnerservice.adapter.`in`.dtos.PartnerDto
import com.example.foodrescue.partnerservice.application.usecases.GetPartnerUseCase
import com.example.foodrescue.partnerservice.application.usecases.CreateOrUpdatePartnerUseCase
import com.example.foodrescue.partnerservice.domain.entity.Partner
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import jakarta.validation.Valid
import org.modelmapper.ModelMapper
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
    private val modelMapper: ModelMapper
) {

    @GetMapping("/{partnerId}")
    fun getPartner(@PathVariable partnerId: UUID): Partner {
        return getPartnerUseCase.getPartner(PartnerId(partnerId))
    }

    @PutMapping()
    fun createOrUpdatePartner(@Valid @RequestBody partner: PartnerDto):Partner {
        return createOrUpdatePartnerUseCase.createOrUpdatePartner(
            modelMapper.map(partner, Partner::class.java)
        )
    }
}
