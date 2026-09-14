package com.example.foodrescue.orderservice.configuration.security

import com.example.foodrescue.orderservice.domain.enum.ApplicationRole
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity
class SecurityConfiguration(private val keycloakRealmRoleConverter: KeycloakRealmRoleConverter) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                csrf.disable()
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { authorization ->
                authorization
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                    )
                    .permitAll()

                authorization
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/orders/*",
                    )
                    .hasAnyRole(
                        ApplicationRole.CUSTOMER.code,
                        ApplicationRole.ADMIN.code,
                    )

                authorization.anyRequest().authenticated()
            }
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(keycloakRealmRoleConverter)
                }
            }

        return http.build()
    }
}
