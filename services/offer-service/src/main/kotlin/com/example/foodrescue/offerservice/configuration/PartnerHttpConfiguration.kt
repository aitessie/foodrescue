package com.example.foodrescue.offerservice.configuration

import java.net.http.HttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver
import org.springframework.web.client.RestClient

@Configuration
class PartnerHttpConfiguration {
    @Bean("partnerServiceClientHttpRequestFactory")
    fun partnerServiceClientHttpRequestFactory(
        properties: PartnerHttpProperties
    ): ClientHttpRequestFactory {
        val httpClient =
            HttpClient.newBuilder()
                .connectTimeout(properties.timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()

        return JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(properties.timeout)
        }
    }

    @Bean("partnerServiceRestClient")
    fun partnerServiceRestClient(
        restClientBuilder: RestClient.Builder,
        properties: PartnerHttpProperties,
        authorizedClientManager: OAuth2AuthorizedClientManager,
        @Qualifier("partnerServiceClientHttpRequestFactory")
        requestFactory: ClientHttpRequestFactory,
    ): RestClient {
        val oauth2Interceptor =
            OAuth2ClientHttpRequestInterceptor(authorizedClientManager).apply {
                setPrincipalResolver(RequestAttributePrincipalResolver())
            }

        return restClientBuilder
            .clone()
            .baseUrl(properties.baseUrl.toString())
            .requestFactory(requestFactory)
            .requestInterceptor(oauth2Interceptor)
            .build()
    }
}
