package com.example.foodrescue.offerservice.adapter.`in`.dtos.validation

import com.example.foodrescue.offerservice.adapter.`in`.dtos.CreateOrUpdateOfferDto
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class OfferPickupWindowValidator :
    ConstraintValidator<ValidOfferPickupWindow, CreateOrUpdateOfferDto> {
    override fun isValid(
        value: CreateOrUpdateOfferDto?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value == null) {
            return true
        }

        if (value.pickupEnd.isAfter(value.pickupStart)) {
            return true
        }

        context.disableDefaultConstraintViolation()
        context
            .buildConstraintViolationWithTemplate("pickupEnd must be later than pickupStart")
            .addPropertyNode("pickupEnd")
            .addConstraintViolation()

        return false
    }
}
