package com.example.foodrescue.offerservice.application.exceptions

import com.example.foodrescue.offerservice.domain.entities.FoodBagId

class FoodBagNotFoundException(foodBagId: FoodBagId) :
    NotFoundException("FoodBag '$foodBagId' was not found")
