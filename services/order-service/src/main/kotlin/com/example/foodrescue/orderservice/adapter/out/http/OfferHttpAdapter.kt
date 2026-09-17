package com.example.foodrescue.orderservice.adapter.out.http

import com.example.foodrescue.orderservice.adapter.out.http.dtos.OfferResponseDto
import com.example.foodrescue.orderservice.adapter.out.http.mappers.OfferHttpMapper
import com.example.foodrescue.orderservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.orderservice.application.exceptions.OfferServiceAuthenticationException
import com.example.foodrescue.orderservice.application.exceptions.OfferServiceContractException
import com.example.foodrescue.orderservice.application.exceptions.OfferServiceUnavailableException
import com.example.foodrescue.orderservice.application.ports.OfferQueryPort
import com.example.foodrescue.orderservice.configuration.OfferHttpProperties
import com.example.foodrescue.orderservice.domain.entities.OfferId
import com.example.foodrescue.orderservice.domain.entities.OfferSnapshot
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
class OfferHttpAdapter(
    @Qualifier("offerServiceRestClient") private val restClient: RestClient,
    private val mapper: OfferHttpMapper,
    private val properties: OfferHttpProperties,
) : OfferQueryPort {
    override fun getOffer(offerId: OfferId): OfferSnapshot {
        try {
            val response =
                restClient
                    .get()
                    .uri(OFFER_PATH, offerId.value)
                    .attributes(clientRegistrationId(properties.oauth2RegistrationId))
                    .attributes(principal(SERVICE_PRINCIPAL_NAME))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError) { _, response ->
                        val status = response.statusCode

                        throw when {
                            status.value() == 400 ->
                                OfferServiceContractException(
                                    "Offer Service rejected the offer snapshot request"
                                )

                            status.value() == 401 || status.value() == 403 ->
                                OfferServiceAuthenticationException()

                            status.value() == 404 -> OfferNotFoundException(offerId)

                            status.is5xxServerError -> OfferServiceUnavailableException()

                            else ->
                                OfferServiceContractException(
                                    "Offer Service returned an unexpected client error"
                                )
                        }
                    }
                    .body(OfferResponseDto::class.java)
                    ?: throw OfferServiceContractException(
                        "Offer Service returned an empty offer snapshot response"
                    )

            if (response.offerId != offerId.value) {
                throw OfferServiceContractException(
                    "Offer Service returned an offer snapshot for a different Offer"
                )
            }

            return mapper.toSnapshot(response)
        } catch (exception: OAuth2AuthorizationException) {
            throw mapOAuth2Exception(exception)
        } catch (exception: ResourceAccessException) {
            throw OfferServiceUnavailableException(cause = exception)
        } catch (exception: RestClientException) {
            throw OfferServiceContractException(
                message = "Offer Service returned an unreadable offer snapshot response",
                cause = exception,
            )
        }
    }

    private fun mapOAuth2Exception(exception: OAuth2AuthorizationException): RuntimeException =
        if (containsConnectionFailure(exception)) {
            OfferServiceUnavailableException(cause = exception)
        } else {
            OfferServiceAuthenticationException(cause = exception)
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
        private const val OFFER_PATH = "/api/v1/offers/{offerId}"
        private const val SERVICE_PRINCIPAL_NAME = "order-service"
    }
}
