package com.example.foodrescue.offerservice.configuration.security

import com.example.foodrescue.offerservice.domain.`enum`.ApplicationRole
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class KeycloakRealmRoleConverterTest {
    private val converter = KeycloakRealmRoleConverter()

    @ParameterizedTest
    @EnumSource(ApplicationRole::class)
    fun whenRealmContainsApplicationRoleExpectedPrefixedAuthority(role: ApplicationRole) {
        // Arrange
        val jwt = createJwt(roles = listOf(role.code))

        // Act
        val authentication = converter.convert(jwt)

        // Assert
        assertThat(authentication).isInstanceOf(JwtAuthenticationToken::class.java)
        assertThat(authentication.authorities)
            .extracting<String> { authority -> authority.authority }
            .containsExactly("ROLE_${role.code}")
    }

    @Test
    fun whenRealmContainsSeveralRolesExpectedAllRolesConverted() {
        // Arrange
        val jwt =
            createJwt(
                roles =
                    listOf(
                        ApplicationRole.CUSTOMER.code,
                        ApplicationRole.STAFF.code,
                        ApplicationRole.MANAGER.code,
                        ApplicationRole.ADMIN.code,
                    )
            )

        // Act
        val authentication = converter.convert(jwt)

        // Assert
        assertThat(authentication.authorities)
            .extracting<String> { authority -> authority.authority }
            .containsExactly(
                "ROLE_CUSTOMER",
                "ROLE_STAFF",
                "ROLE_MANAGER",
                "ROLE_ADMIN",
            )
    }

    @Test
    fun whenPreferredUsernamePresentExpectedAuthenticationNameUsesUsername() {
        // Arrange
        val jwt =
            createJwt(
                roles = listOf(ApplicationRole.MANAGER.code),
                preferredUsername = "manager.demo",
                subject = "11111111-1111-1111-1111-111111111111",
            )

        // Act
        val authentication = converter.convert(jwt)

        // Assert
        assertThat(authentication.name).isEqualTo("manager.demo")
        assertThat((authentication as JwtAuthenticationToken).token.subject)
            .isEqualTo("11111111-1111-1111-1111-111111111111")
    }

    @Test
    fun whenPreferredUsernameMissingExpectedAuthenticationNameUsesSubject() {
        // Arrange
        val jwt =
            createJwt(
                roles = listOf(ApplicationRole.STAFF.code),
                preferredUsername = null,
                subject = "22222222-2222-2222-2222-222222222222",
            )

        // Act
        val authentication = converter.convert(jwt)

        // Assert
        assertThat(authentication.name).isEqualTo("22222222-2222-2222-2222-222222222222")
    }

    @Test
    fun whenRealmAccessMissingExpectedNoAuthorities() {
        // Arrange
        val jwt =
            Jwt.withTokenValue("access-token")
                .header("alg", "none")
                .subject("99999999-9999-9999-9999-999999999999")
                .claim(
                    "preferred_username",
                    "no-role.demo",
                )
                .issuedAt(Instant.parse("2026-08-18T10:00:00Z"))
                .expiresAt(Instant.parse("2026-08-18T11:00:00Z"))
                .build()

        // Act
        val authentication = converter.convert(jwt)

        // Assert
        assertThat(authentication.name).isEqualTo("no-role.demo")
        assertThat(authentication.authorities).isEmpty()
    }

    @Test
    fun whenRealmRolesMissingExpectedNoAuthorities() {
        // Arrange
        val jwt =
            Jwt.withTokenValue("access-token")
                .header("alg", "none")
                .subject("99999999-9999-9999-9999-999999999999")
                .claim(
                    "preferred_username",
                    "no-role.demo",
                )
                .claim(
                    "realm_access",
                    emptyMap<String, Any>(),
                )
                .issuedAt(Instant.parse("2026-08-18T10:00:00Z"))
                .expiresAt(Instant.parse("2026-08-18T11:00:00Z"))
                .build()

        // Act
        val authentication = converter.convert(jwt)

        // Assert
        assertThat(authentication.authorities).isEmpty()
    }

    @Test
    fun whenRealmRolesContainNonStringValuesExpectedInvalidValuesIgnored() {
        // Arrange
        val jwt =
            createJwt(
                roles =
                    listOf(
                        ApplicationRole.ADMIN.code,
                        42,
                        true,
                        mapOf("name" to "STAFF"),
                    )
            )

        // Act
        val authentication = converter.convert(jwt)

        // Assert
        assertThat(authentication.authorities)
            .extracting<String> { authority -> authority.authority }
            .containsExactly("ROLE_ADMIN")
    }

    @Test
    fun whenRealmContainsUnknownStringRoleExpectedRoleStillConverted() {
        // Arrange
        val jwt =
            createJwt(
                roles =
                    listOf(
                        "OFFER_SERVICE",
                        "UNKNOWN_ROLE",
                    )
            )

        // Act
        val authentication = converter.convert(jwt)

        // Assert
        assertThat(authentication.authorities)
            .extracting<String> { authority -> authority.authority }
            .containsExactly(
                "ROLE_OFFER_SERVICE",
                "ROLE_UNKNOWN_ROLE",
            )
    }

    private fun createJwt(
        roles: List<Any>,
        preferredUsername: String? = "manager.demo",
        subject: String = "11111111-1111-1111-1111-111111111111",
    ): Jwt {
        val builder =
            Jwt.withTokenValue("access-token")
                .header("alg", "none")
                .subject(subject)
                .claim(
                    "realm_access",
                    mapOf("roles" to roles),
                )
                .issuedAt(Instant.parse("2026-08-18T10:00:00Z"))
                .expiresAt(Instant.parse("2026-08-18T11:00:00Z"))

        if (preferredUsername != null) {
            builder.claim(
                "preferred_username",
                preferredUsername,
            )
        }

        return builder.build()
    }
}
