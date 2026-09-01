package com.example.foodrescue.offerservice.adapter.`in`.dtos.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [OfferPickupWindowValidator::class])
annotation class ValidOfferPickupWindow(
    val message: String = "pickupEnd must be later than pickupStart",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
