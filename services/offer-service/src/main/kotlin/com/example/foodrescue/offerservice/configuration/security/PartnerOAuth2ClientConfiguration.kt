package com.example.foodrescue.offerservice.configuration.security

import com.example.foodrescue.offerservice.configuration.PartnerHttpProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter
import org.springframework.web.client.RestClient

@Configuration
class PartnerOAuth2ClientConfiguration {
    @Bean
    fun partnerServiceClientCredentialsTokenResponseClient(
        @Qualifier("partnerServiceClientHttpRequestFactory")
        requestFactory: ClientHttpRequestFactory
    ): OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> {
        val tokenRestClient =
            RestClient.builder()
                .requestFactory(requestFactory)
                .configureMessageConverters { converters ->
                    converters.addCustomConverter(FormHttpMessageConverter())
                    converters.addCustomConverter(OAuth2AccessTokenResponseHttpMessageConverter())
                }
                .defaultStatusHandler(OAuth2ErrorResponseErrorHandler())
                .build()

        return RestClientClientCredentialsTokenResponseClient().apply {
            setRestClient(tokenRestClient)
        }
    }

    @Bean
    fun partnerServiceAuthorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService,
        clientCredentialsTokenResponseClient:
            OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest>,
        partnerHttpProperties: PartnerHttpProperties,
    ): OAuth2AuthorizedClientManager {
        require(partnerHttpProperties.oauth2RegistrationId == "partner-service") {
            "Partner OAuth2 registration id must be partner-service"
        }

        requireNotNull(
            clientRegistrationRepository.findByRegistrationId(
                partnerHttpProperties.oauth2RegistrationId
            )
        ) {
            "OAuth2 client registration partner-service is required"
        }

        val authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials { clientCredentials ->
                    clientCredentials.accessTokenResponseClient(
                        clientCredentialsTokenResponseClient
                    )
                }
                .build()

        return AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService,
            )
            .apply {
                setAuthorizedClientProvider(authorizedClientProvider)
            }
    }
}
