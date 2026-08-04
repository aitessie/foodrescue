package com.example.foodrescue.partnerservice.adapter.out

import com.example.foodrescue.partnerservice.application.ports.CurrentUserPort
import com.example.foodrescue.partnerservice.domain.enum.ApplicationRole
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class CurrentUserAdapterService : CurrentUserPort {
    override fun getUserId(): String {
        return getAuthentication().token.subject!!
    }

    override fun hasRole(role: ApplicationRole): Boolean {
        val expectedAuthority = "ROLE_${role.code}"

        return getAuthentication()
            .authorities
            .any { authority ->
                authority.authority == expectedAuthority
            }
    }

    private fun getAuthentication(): JwtAuthenticationToken {
        val authentication =
            SecurityContextHolder.getContext().authentication

        if (
            authentication !is JwtAuthenticationToken ||
            !authentication.isAuthenticated
        ) {
            throw AuthenticationCredentialsNotFoundException(
                "Authenticated user was not found"
            )
        }

        return authentication
    }
}
