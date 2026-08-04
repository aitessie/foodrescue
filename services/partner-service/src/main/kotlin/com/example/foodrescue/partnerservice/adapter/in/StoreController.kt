package com.example.foodrescue.partnerservice.adapter.`in`

import com.example.foodrescue.partnerservice.adapter.`in`.dtos.StoreDto
import com.example.foodrescue.partnerservice.application.usecases.GetStoreUseCase
import com.example.foodrescue.partnerservice.application.usecases.CreateOrUpdateStoreUseCase
import com.example.foodrescue.partnerservice.domain.entity.Store
import com.example.foodrescue.partnerservice.domain.entity.StoreId
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
@RequestMapping("/api/v1/stores")
class StoreController(
    private val getStoreUseCase: GetStoreUseCase,
    private val createOrUpdateStoreUseCase: CreateOrUpdateStoreUseCase,
    private val modelMapper: ModelMapper
) {

    @GetMapping("/{storeId}")
    fun getStore(@PathVariable storeId: UUID): Store {
        return getStoreUseCase.getStore(StoreId(storeId))
    }

    @PutMapping
    fun createOrUpdateStore(@Valid @RequestBody store: StoreDto): Store {
        return createOrUpdateStoreUseCase.createOrUpdateStore(
            modelMapper.map(store, Store::class.java)
        )
    }
}
