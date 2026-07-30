package com.example.foodrescue.partner.configuration.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class KeycloakRealmRoleConverter :
    Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val realmAccess = jwt.getClaimAsMap("realm_access").orEmpty()

        val roles = (realmAccess["roles"] as? Collection<*>)
            .orEmpty()
            .filterIsInstance<String>()

        val authorities = roles.map { role ->
            SimpleGrantedAuthority("ROLE_$role")
        }

        val principalName = requireNotNull(jwt.getClaimAsString("preferred_username") ?: jwt.subject)

        return JwtAuthenticationToken(
            jwt,
            authorities,
            principalName,
        )
    }
}
