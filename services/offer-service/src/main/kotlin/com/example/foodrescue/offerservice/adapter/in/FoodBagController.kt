package com.example.foodrescue.offerservice.adapter.`in`

import com.example.foodrescue.offerservice.adapter.`in`.dtos.FoodBagDto
import com.example.foodrescue.offerservice.adapter.`in`.dtos.FoodBagStatusDto
import com.example.foodrescue.offerservice.adapter.`in`.mappers.FoodBagRestMapper
import com.example.foodrescue.offerservice.application.usecases.ChangeFoodBagStatusUseCase
import com.example.foodrescue.offerservice.application.usecases.CreateOrUpdateFoodBagUseCase
import com.example.foodrescue.offerservice.application.usecases.GetFoodBagUseCase
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
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
@RequestMapping("/api/v1/partners/{partnerId}/stores/{storeId}/food-bags")
class FoodBagController(
    private val createOrUpdateFoodBagUseCase: CreateOrUpdateFoodBagUseCase,
    private val getFoodBagUseCase: GetFoodBagUseCase,
    private val changeFoodBagStatusUseCase: ChangeFoodBagStatusUseCase,
    private val foodBagRestMapper: FoodBagRestMapper,
) {
    @GetMapping("/{foodBagId}")
    fun getFoodBag(
        @PathVariable partnerId: UUID,
        @PathVariable storeId: UUID,
        @PathVariable foodBagId: UUID,
    ): FoodBagDto {
        val foodBag =
            getFoodBagUseCase.execute(
                PartnerId(partnerId),
                StoreId(storeId),
                FoodBagId(foodBagId),
            )

        return foodBagRestMapper.toDto(foodBag)
    }

    @PutMapping("/{foodBagId}")
    fun createOrUpdateFoodBag(
        @PathVariable partnerId: UUID,
        @PathVariable storeId: UUID,
        @PathVariable foodBagId: UUID,
        @Valid @RequestBody dto: FoodBagDto,
    ): FoodBagDto {
        val source =
            foodBagRestMapper.toDomain(
                storeId = storeId,
                foodBagId = foodBagId,
                dto = dto,
            )

        val foodBag =
            createOrUpdateFoodBagUseCase.execute(
                PartnerId(partnerId),
                source,
            )

        return foodBagRestMapper.toDto(foodBag)
    }

    @GetMapping("/{foodBagId}/status")
    fun getFoodBagStatus(
        @PathVariable partnerId: UUID,
        @PathVariable storeId: UUID,
        @PathVariable foodBagId: UUID,
    ): FoodBagStatusDto {
        val foodBag =
            getFoodBagUseCase.execute(
                PartnerId(partnerId),
                StoreId(storeId),
                FoodBagId(foodBagId),
            )

        return foodBagRestMapper.toStatusDto(foodBag)
    }

    @PutMapping("/{foodBagId}/status")
    fun changeFoodBagStatus(
        @PathVariable partnerId: UUID,
        @PathVariable storeId: UUID,
        @PathVariable foodBagId: UUID,
        @Valid @RequestBody dto: FoodBagStatusDto,
    ): FoodBagStatusDto {
        val foodBag =
            changeFoodBagStatusUseCase.execute(
                PartnerId(partnerId),
                StoreId(storeId),
                FoodBagId(foodBagId),
                dto.status,
                dto.version,
            )

        return foodBagRestMapper.toStatusDto(foodBag)
    }
}
