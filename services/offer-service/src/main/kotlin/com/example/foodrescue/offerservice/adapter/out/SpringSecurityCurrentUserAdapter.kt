package com.example.foodrescue.offerservice.adapter.out

import com.example.foodrescue.offerservice.application.ports.CurrentUserPort
import com.example.foodrescue.offerservice.domain.`enum`.ApplicationRole
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class SpringSecurityCurrentUserAdapter : CurrentUserPort {
    override fun getUserId(): String {
        val subject = currentAuthentication().token.subject

        if (subject.isNullOrBlank()) {
            throw IllegalStateException("Authenticated JWT subject must not be blank")
        }

        return subject
    }

    override fun hasRole(role: ApplicationRole): Boolean =
        currentAuthentication().authorities.any { authority ->
            authority.authority == "ROLE_${role.code}"
        }

    private fun currentAuthentication(): JwtAuthenticationToken {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication !is JwtAuthenticationToken || !authentication.isAuthenticated) {
            throw IllegalStateException("Authenticated user JWT is required")
        }

        return authentication
    }
}
