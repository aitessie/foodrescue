package com.example.foodrescue.offerservice.application.ports

import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.FoodBagId

interface FoodBagDBPort {
    fun findById(id: FoodBagId): FoodBag?

    fun save(foodBag: FoodBag): FoodBag
}
