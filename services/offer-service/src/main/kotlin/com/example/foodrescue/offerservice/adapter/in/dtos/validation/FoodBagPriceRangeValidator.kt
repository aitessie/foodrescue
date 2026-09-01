package com.example.foodrescue.offerservice.adapter.`in`.dtos.validation

import com.example.foodrescue.offerservice.adapter.`in`.dtos.FoodBagDto
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class FoodBagPriceRangeValidator : ConstraintValidator<ValidFoodBagPriceRange, FoodBagDto> {
    override fun isValid(
        value: FoodBagDto?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value == null) {
            return true
        }

        if (value.unitPriceMinor < value.originalPriceMinor) {
            return true
        }

        context.disableDefaultConstraintViolation()
        context
            .buildConstraintViolationWithTemplate(
                "unitPriceMinor must be less than originalPriceMinor"
            )
            .addPropertyNode("unitPriceMinor")
            .addConstraintViolation()

        return false
    }
}
