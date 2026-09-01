package com.example.foodrescue.offerservice.configuration.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.DefaultSecurityFilterChain

@ExtendWith(MockitoExtension::class)
class SecurityConfigurationTest {
    @Mock private lateinit var keycloakRealmRoleConverter: KeycloakRealmRoleConverter

    @Mock private lateinit var http: HttpSecurity

    @Mock private lateinit var csrfConfigurer: CsrfConfigurer<HttpSecurity>

    @Mock private lateinit var sessionConfigurer: SessionManagementConfigurer<HttpSecurity>

    @Mock
    private lateinit var authorizationRegistry:
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry

    @Mock
    private lateinit var authorizedUrl: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl

    @Mock
    private lateinit var resourceServerConfigurer: OAuth2ResourceServerConfigurer<HttpSecurity>

    @Mock
    private lateinit var jwtConfigurer: OAuth2ResourceServerConfigurer<HttpSecurity>.JwtConfigurer

    @Mock private lateinit var securityFilterChain: DefaultSecurityFilterChain

    @InjectMocks private lateinit var configuration: SecurityConfiguration

    @Test
    fun whenSecurityFilterChainIsConfigured_returnsConfiguredFilterChain() {
        // Arrange
        doAnswer { invocation ->
                val customizer = invocation.getArgument<Customizer<CsrfConfigurer<HttpSecurity>>>(0)

                customizer.customize(csrfConfigurer)
                http
            }
            .`when`(http)
            .csrf(any<Customizer<CsrfConfigurer<HttpSecurity>>>())

        doAnswer { invocation ->
                val customizer =
                    invocation.getArgument<Customizer<SessionManagementConfigurer<HttpSecurity>>>(0)

                customizer.customize(sessionConfigurer)
                http
            }
            .`when`(http)
            .sessionManagement(any<Customizer<SessionManagementConfigurer<HttpSecurity>>>())

        `when`(
                authorizationRegistry.requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                )
            )
            .thenReturn(authorizedUrl)
        `when`(authorizationRegistry.requestMatchers("/api/v1/offers/**")).thenReturn(authorizedUrl)
        `when`(authorizationRegistry.requestMatchers("/api/v1/partners/**"))
            .thenReturn(authorizedUrl)
        `when`(authorizationRegistry.anyRequest()).thenReturn(authorizedUrl)

        doAnswer { invocation ->
                val customizer =
                    invocation.getArgument<
                        Customizer<
                            AuthorizeHttpRequestsConfigurer<
                                HttpSecurity
                            >.AuthorizationManagerRequestMatcherRegistry
                        >
                    >(
                        0
                    )

                customizer.customize(authorizationRegistry)
                http
            }
            .`when`(http)
            .authorizeHttpRequests(
                any<
                    Customizer<
                        AuthorizeHttpRequestsConfigurer<
                            HttpSecurity
                        >.AuthorizationManagerRequestMatcherRegistry
                    >
                >()
            )

        doAnswer { invocation ->
                val customizer =
                    invocation.getArgument<
                        Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>.JwtConfigurer>
                    >(
                        0
                    )

                customizer.customize(jwtConfigurer)
                resourceServerConfigurer
            }
            .`when`(resourceServerConfigurer)
            .jwt(any<Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>.JwtConfigurer>>())

        doAnswer { invocation ->
                val customizer =
                    invocation.getArgument<
                        Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>>
                    >(
                        0
                    )

                customizer.customize(resourceServerConfigurer)
                http
            }
            .`when`(http)
            .oauth2ResourceServer(any<Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>>>())

        `when`(http.build()).thenReturn(securityFilterChain)

        // Act
        val result = configuration.securityFilterChain(http)

        // Assert
        assertThat(result).isSameAs(securityFilterChain)

        verify(http).csrf(any<Customizer<CsrfConfigurer<HttpSecurity>>>())
        verify(csrfConfigurer).disable()

        verify(http).sessionManagement(any<Customizer<SessionManagementConfigurer<HttpSecurity>>>())
        verify(sessionConfigurer).sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        verify(http)
            .authorizeHttpRequests(
                any<
                    Customizer<
                        AuthorizeHttpRequestsConfigurer<
                            HttpSecurity
                        >.AuthorizationManagerRequestMatcherRegistry
                    >
                >()
            )
        verify(authorizationRegistry)
            .requestMatchers(
                "/actuator/health",
                "/actuator/health/**",
            )
        verify(authorizationRegistry).requestMatchers("/api/v1/offers/**")
        verify(authorizationRegistry).requestMatchers("/api/v1/partners/**")
        verify(authorizationRegistry).anyRequest()
        verify(authorizedUrl).permitAll()
        verify(authorizedUrl, times(3)).authenticated()

        verify(http)
            .oauth2ResourceServer(any<Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>>>())
        verify(resourceServerConfigurer)
            .jwt(any<Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>.JwtConfigurer>>())
        verify(jwtConfigurer).jwtAuthenticationConverter(keycloakRealmRoleConverter)

        verify(http).build()

        verifyNoMoreInteractions(
            http,
            csrfConfigurer,
            sessionConfigurer,
            authorizationRegistry,
            authorizedUrl,
            resourceServerConfigurer,
            jwtConfigurer,
            keycloakRealmRoleConverter,
            securityFilterChain,
        )
    }
}
