package com.example.foodrescue.offerservice.adapter.out.http

import com.example.foodrescue.offerservice.adapter.out.http.dtos.PartnerStoreAccessResponseDto
import com.example.foodrescue.offerservice.adapter.out.http.mappers.PartnerHttpMapper
import com.example.foodrescue.offerservice.application.exceptions.PartnerServiceAuthenticationException
import com.example.foodrescue.offerservice.application.exceptions.PartnerServiceContractException
import com.example.foodrescue.offerservice.application.exceptions.PartnerServiceUnavailableException
import com.example.foodrescue.offerservice.application.exceptions.PartnerStoreNotFoundException
import com.example.foodrescue.offerservice.application.ports.PartnerStoreAccessPort
import com.example.foodrescue.offerservice.configuration.PartnerHttpProperties
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.PartnerStoreAccessSnapshot
import com.example.foodrescue.offerservice.domain.entities.StoreId
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId
import org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver.principal
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class PartnerHttpAdapter(
    @Qualifier("partnerServiceRestClient") private val restClient: RestClient,
    private val mapper: PartnerHttpMapper,
    private val properties: PartnerHttpProperties,
) : PartnerStoreAccessPort {
    override fun checkAccess(
        partnerId: PartnerId,
        storeId: StoreId,
        userId: String,
    ): PartnerStoreAccessSnapshot {
        val request =
            mapper.toRequest(
                partnerId = partnerId,
                storeId = storeId,
                userId = userId,
            )

        try {
            val response =
                restClient
                    .post()
                    .uri(PARTNER_STORE_ACCESS_PATH)
                    .attributes(clientRegistrationId(properties.oauth2RegistrationId))
                    .attributes(principal(SERVICE_PRINCIPAL_NAME))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError) { _, response ->
                        val status = response.statusCode

                        throw when {
                            status.value() == 400 ->
                                PartnerServiceContractException(
                                    "Partner Service rejected the access-check request"
                                )

                            status.value() == 401 || status.value() == 403 ->
                                PartnerServiceAuthenticationException()

                            status.value() == 404 ->
                                PartnerStoreNotFoundException(
                                    partnerId = partnerId,
                                    storeId = storeId,
                                )

                            status.is5xxServerError -> PartnerServiceUnavailableException()

                            else ->
                                PartnerServiceContractException(
                                    "Partner Service returned an unexpected client error"
                                )
                        }
                    }
                    .body(PartnerStoreAccessResponseDto::class.java)
                    ?: throw PartnerServiceContractException(
                        "Partner Service returned an empty access-check response"
                    )

            return mapper.toSnapshot(response)
        } catch (exception: OAuth2AuthorizationException) {
            throw mapOAuth2Exception(exception)
        } catch (exception: ResourceAccessException) {
            throw PartnerServiceUnavailableException(cause = exception)
        } catch (exception: RestClientException) {
            throw PartnerServiceContractException(
                message = "Partner Service returned an unreadable access-check response",
                cause = exception,
            )
        }
    }

    private fun mapOAuth2Exception(exception: OAuth2AuthorizationException): RuntimeException =
        if (containsConnectionFailure(exception)) {
            PartnerServiceUnavailableException(cause = exception)
        } else {
            PartnerServiceAuthenticationException(cause = exception)
        }

    private fun containsConnectionFailure(exception: Throwable): Boolean {
        var current: Throwable? = exception

        while (current != null) {
            if (current is ResourceAccessException) {
                return true
            }
            current = current.cause
        }

        return false
    }

    companion object {
        private const val PARTNER_STORE_ACCESS_PATH = "/internal/api/v1/partner-store-access/check"
        private const val SERVICE_PRINCIPAL_NAME = "offer-service"
    }
}
