package com.example.foodrescue.orderservice.adapter.out.tokens

import com.example.foodrescue.orderservice.application.ports.PickupTokenHashPort
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat
import org.springframework.stereotype.Component

@Component
class Sha256PickupTokenHashAdapter : PickupTokenHashPort {
    override fun hash(token: String): String =
        HexFormat.of().formatHex(
            MessageDigest.getInstance(HASH_ALGORITHM)
                .digest(token.toByteArray(StandardCharsets.UTF_8))
        )

    private companion object {
        private const val HASH_ALGORITHM = "SHA-256"
    }
}
