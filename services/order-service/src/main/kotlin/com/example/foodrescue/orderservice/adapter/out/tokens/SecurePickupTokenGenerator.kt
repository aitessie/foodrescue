package com.example.foodrescue.orderservice.adapter.out.tokens

import com.example.foodrescue.orderservice.application.ports.PickupTokenGeneratorPort
import java.security.SecureRandom
import java.util.Base64
import org.springframework.stereotype.Component

@Component
class SecurePickupTokenGenerator : PickupTokenGeneratorPort {
    private val secureRandom = SecureRandom()

    override fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        private const val TOKEN_BYTES = 16
    }
}
