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

        if (value.unitPrice < value.originalPrice) {
            return true
        }

        context.disableDefaultConstraintViolation()
        context
            .buildConstraintViolationWithTemplate(
                "unitPrice must be less than originalPrice",
            )
            .addPropertyNode("unitPrice")
            .addConstraintViolation()

        return false
    }
}
