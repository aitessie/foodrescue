package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.access.FoodBagAccessPolicy
import com.example.foodrescue.offerservice.application.exceptions.FoodBagNotFoundException
import com.example.foodrescue.offerservice.application.ports.FoodBagDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetFoodBagUseCase(
    private val foodBagDBPort: FoodBagDBPort,
    private val accessPolicy: FoodBagAccessPolicy,
) {
    @Transactional(readOnly = true)
    fun execute(
        partnerId: PartnerId,
        storeId: StoreId,
        foodBagId: FoodBagId,
    ): FoodBag {
        val foodBag = foodBagDBPort.findById(foodBagId) ?: throw FoodBagNotFoundException(foodBagId)

        if (foodBag.storeId != storeId) {
            throw FoodBagNotFoundException(foodBagId)
        }

        accessPolicy.checkAccess(
            partnerId = partnerId,
            storeId = storeId,
        )

        return foodBag
    }
}
