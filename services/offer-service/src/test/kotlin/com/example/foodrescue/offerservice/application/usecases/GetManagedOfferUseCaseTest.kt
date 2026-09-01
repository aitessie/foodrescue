package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.access.OfferAccessPolicy
import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.OfferNotFoundException
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GetManagedOfferUseCaseTest {
    @Mock private lateinit var offerDBPort: OfferDBPort

    @Mock private lateinit var accessPolicy: OfferAccessPolicy

    @InjectMocks private lateinit var useCase: GetManagedOfferUseCase

    @Test
    fun whenUserRequestsExistingOffer_returnsOffer() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer(storeId = storeId)

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                storeId = storeId,
                offerId = offer.id,
            )

        // Assert
        assertThat(result).isSameAs(offer)

        verify(offerDBPort).findById(offer.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verifyNoMoreInteractions(
            offerDBPort,
            accessPolicy,
        )
    }

    @Test
    fun whenUserRequestsMissingOffer_throwsOfferNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offerId = OfferId(UUID.randomUUID())

        `when`(offerDBPort.findById(offerId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offerId,
                )
            }

        // Assert
        assertThat(exception.message).contains(offerId.value.toString())

        verify(offerDBPort).findById(offerId)
        verifyNoInteractions(accessPolicy)
        verifyNoMoreInteractions(offerDBPort)
    }

    @Test
    fun whenUserRequestsOfferFromAnotherStore_throwsOfferNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer()

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)

        // Act
        val exception =
            assertThrows<OfferNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offer.id,
                )
            }

        // Assert
        assertThat(exception.message).contains(offer.id.value.toString())

        verify(offerDBPort).findById(offer.id)
        verifyNoInteractions(accessPolicy)
        verifyNoMoreInteractions(offerDBPort)
    }

    @Test
    fun whenUserCannotManageStore_throwsAccessDeniedException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val offer = createOffer(storeId = storeId)
        val expectedException = AccessDeniedException()

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        doThrow(expectedException)
            .`when`(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    offerId = offer.id,
                )
            }

        // Assert
        assertThat(exception).isSameAs(expectedException)

        verify(offerDBPort).findById(offer.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verifyNoMoreInteractions(
            offerDBPort,
            accessPolicy,
        )
    }

    private fun createOffer(
        id: OfferId = OfferId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        foodBagId: FoodBagId = FoodBagId(UUID.randomUUID()),
    ): Offer =
        Offer(
            id = id,
            storeId = storeId,
            foodBagId = foodBagId,
            category = FoodBagCategory.entries.first(),
            unitPrice =
                Money(
                    amountMinor = 500,
                    currency = MoneyCurrency.RUB,
                ),
            allergens = emptySet(),
            status = OfferStatus.ACTIVE,
            totalQuantity = 5,
            availableQuantity = 5,
            pickupWindow =
                PickupWindow(
                    start = Instant.parse("2026-08-20T12:00:00Z"),
                    end = Instant.parse("2026-08-20T14:00:00Z"),
                ),
            createdAt = Instant.parse("2026-08-20T10:00:00Z"),
            updatedAt = Instant.parse("2026-08-20T10:00:00Z"),
            version = 0,
        )
}
