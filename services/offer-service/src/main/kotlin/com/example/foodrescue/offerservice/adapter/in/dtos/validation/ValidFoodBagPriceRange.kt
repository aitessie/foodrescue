package com.example.foodrescue.offerservice.adapter.`in`.dtos.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [FoodBagPriceRangeValidator::class])
annotation class ValidFoodBagPriceRange(
    val message: String = "unitPriceMinor must be less than originalPriceMinor",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
