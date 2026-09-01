package com.example.foodrescue.offerservice.adapter.out.db

import com.example.foodrescue.offerservice.application.ports.OfferSearchSettingsPort
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "food-rescue.search")
data class OfferSearchProperties(
    override val minLimit: Int,
    override val maxLimit: Int,
) : OfferSearchSettingsPort {
    init {
        require(minLimit > 0) {
            "food-rescue.search.min-limit must be positive"
        }

        require(maxLimit >= minLimit) {
            "food-rescue.search.max-limit must not be less than min-limit"
        }
    }
}
