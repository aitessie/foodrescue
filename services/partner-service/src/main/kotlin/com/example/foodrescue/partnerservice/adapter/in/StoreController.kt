package com.example.foodrescue.partnerservice.adapter.`in`

import com.example.foodrescue.partnerservice.adapter.`in`.dtos.StoreDto
import com.example.foodrescue.partnerservice.adapter.`in`.mapper.StoreRestMapper
import com.example.foodrescue.partnerservice.application.usecases.GetStoreUseCase
import com.example.foodrescue.partnerservice.application.usecases.CreateOrUpdateStoreUseCase
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import com.example.foodrescue.partnerservice.domain.entity.StoreId
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/partners/{partnerId}/stores")
class StoreController(
    private val getStoreUseCase: GetStoreUseCase,
    private val createOrUpdateStoreUseCase: CreateOrUpdateStoreUseCase,
    private val storeRestMapper: StoreRestMapper
) {

    @GetMapping("/{storeId}")
    fun getStore(
        @PathVariable partnerId: UUID,
        @PathVariable storeId: UUID,
    ): StoreDto {
        val store = getStoreUseCase.getStore(
            partnerId = PartnerId(partnerId),
            storeId = StoreId(storeId),
        )

        return storeRestMapper.toDto(store)
    }

    @PutMapping("/{storeId}")
    fun createOrUpdateStore(
        @PathVariable partnerId: UUID,
        @PathVariable storeId: UUID,
        @Valid @RequestBody storeDto: StoreDto,
    ): StoreDto {
        val source = storeRestMapper.toDomain(
            dto = storeDto,
            partnerId = PartnerId(partnerId),
            storeId = StoreId(storeId),
        )

        val savedStore = createOrUpdateStoreUseCase.createOrUpdateStore(source)

        return storeRestMapper.toDto(savedStore)
    }
}
