package com.example.foodrescue.partnerservice.adapter.`in`

import com.example.foodrescue.partnerservice.adapter.`in`.dtos.StoreDto
import com.example.foodrescue.partnerservice.adapter.`in`.mapper.StoreRestMapper
import com.example.foodrescue.partnerservice.application.usecases.GetStoreUseCase
import com.example.foodrescue.partnerservice.application.usecases.CreateOrUpdateStoreUseCase
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
@RequestMapping("/api/v1/stores")
class StoreController(
    private val getStoreUseCase: GetStoreUseCase,
    private val createOrUpdateStoreUseCase: CreateOrUpdateStoreUseCase,
    private val storeRestMapper: StoreRestMapper
) {

    @GetMapping("/{storeId}")
    fun getStore(@PathVariable storeId: UUID): StoreDto {
        val store = getStoreUseCase.getStore(StoreId(storeId))
        return storeRestMapper.toDto(store)
    }

    @PutMapping
    fun createOrUpdateStore(@Valid @RequestBody store: StoreDto): StoreDto {
        val store = storeRestMapper.toDomain(store)

        return storeRestMapper.toDto(createOrUpdateStoreUseCase.createOrUpdateStore(store))
    }
}
