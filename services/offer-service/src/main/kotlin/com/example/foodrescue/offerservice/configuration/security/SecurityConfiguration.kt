package com.example.foodrescue.offerservice.configuration.security

import com.example.foodrescue.offerservice.domain.enum.ApplicationRole
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
                        HttpMethod.PUT,
                        "/api/v1/offers/*/reservations/*",
                    )
                    .hasAnyRole(
                        ApplicationRole.CUSTOMER.code,
                        ApplicationRole.ADMIN.code,
                    )

                authorization
                    .requestMatchers(
                        "/api/v1/reservations/**",
                    )
                    .hasAnyRole(
                        ApplicationRole.CUSTOMER.code,
                        ApplicationRole.ADMIN.code,
                    )

                authorization
                    .requestMatchers(
                        "/api/v1/partners/**",
                    )
                    .hasAnyRole(
                        ApplicationRole.STAFF.code,
                        ApplicationRole.MANAGER.code,
                        ApplicationRole.ADMIN.code,
                    )

                authorization
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/offers",
                        "/api/v1/offers/*",
                    )
                    .authenticated()

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
