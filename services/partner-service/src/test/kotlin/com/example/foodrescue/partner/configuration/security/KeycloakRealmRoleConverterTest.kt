package com.example.foodrescue.partner.configuration.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

class KeycloakRealmRoleConverterTest {

    private val converter = KeycloakRealmRoleConverter()

    @Test
    fun convertsRealmRolesToSpringAuthorities() {
        val jwt = Jwt
            .withTokenValue("test-token")
            .header("alg", "none")
            .subject("user-id-123")
            .claim("preferred_username", "manager.demo")
            .claim(
                "realm_access",
                mapOf(
                    "roles" to listOf("STORE_MANAGER", "CUSTOMER"),
                ),
            )
            .build()

        val authentication = converter.convert(jwt)

        val authorities = authentication.authorities
            .map { authority -> authority.authority }
            .toSet()

        assertTrue("ROLE_STORE_MANAGER" in authorities)
        assertTrue("ROLE_CUSTOMER" in authorities)
        assertEquals("manager.demo", authentication.name)
    }

    @Test
    fun returnsEmptyAuthoritiesWhenRealmAccessIsAbsent() {
        val jwt = Jwt
            .withTokenValue("test-token")
            .header("alg", "none")
            .subject("user-id-123")
            .build()

        val authentication = converter.convert(jwt)

        assertTrue(authentication.authorities.isEmpty())
        assertEquals("user-id-123", authentication.name)
    }
}
