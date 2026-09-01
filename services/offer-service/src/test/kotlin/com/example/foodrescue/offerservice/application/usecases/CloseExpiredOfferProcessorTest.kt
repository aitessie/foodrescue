package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.application.ports.OfferDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.Offer
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.PickupWindow
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
import com.example.foodrescue.offerservice.domain.`enum`.OfferStatus
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CloseExpiredOfferProcessorTest {
    @Mock private lateinit var offerDBPort: OfferDBPort

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @InjectMocks private lateinit var processor: CloseExpiredOfferProcessor

    @Test
    fun whenExpiredOfferIsProcessed_returnsTrue() {
        // Arrange
        val offer = createOffer()
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedOffer =
            createOffer(
                id = offer.id,
                storeId = offer.storeId,
                foodBagId = offer.foodBagId,
                status = OfferStatus.CLOSED,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .offerClosed(
                    offer = savedOffer,
                    occurredAt = now,
                )

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)
        `when`(offerDBPort.save(offer)).thenReturn(savedOffer)
        `when`(
                eventFactory.offerClosed(
                    offer = savedOffer,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result =
            processor.closeIfExpired(
                offerId = offer.id,
                now = now,
            )

        // Assert
        assertThat(result).isTrue()
        assertThat(offer.status).isEqualTo(OfferStatus.CLOSED)
        assertThat(offer.updatedAt).isEqualTo(now)

        verify(offerDBPort).findById(offer.id)
        verify(offerDBPort).save(offer)
        verify(eventFactory)
            .offerClosed(
                offer = savedOffer,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            offerDBPort,
            eventFactory,
            eventPublisherPort,
        )
    }

    @Test
    fun whenOfferDoesNotExist_returnsFalse() {
        // Arrange
        val offerId = OfferId(UUID.randomUUID())
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(offerDBPort.findById(offerId)).thenReturn(null)

        // Act
        val result =
            processor.closeIfExpired(
                offerId = offerId,
                now = now,
            )

        // Assert
        assertThat(result).isFalse()

        verify(offerDBPort).findById(offerId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(offerDBPort)
    }

    @Test
    fun whenOfferIsNotExpired_returnsFalse() {
        // Arrange
        val offer =
            createOffer(
                pickupWindow =
                    PickupWindow(
                        start = Instant.parse("2026-08-20T12:00:00Z"),
                        end = Instant.parse("2026-08-20T14:00:00Z"),
                    )
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)

        // Act
        val result =
            processor.closeIfExpired(
                offerId = offer.id,
                now = now,
            )

        // Assert
        assertThat(result).isFalse()
        assertThat(offer.status).isEqualTo(OfferStatus.ACTIVE)
        assertThat(offer.updatedAt).isEqualTo(Instant.parse("2026-08-20T10:00:00Z"))

        verify(offerDBPort).findById(offer.id)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(offerDBPort)
    }

    @ParameterizedTest
    @EnumSource(
        value = OfferStatus::class,
        names =
            [
                "CLOSED",
                "CANCELLED",
            ],
    )
    fun whenTerminalOfferIsProcessed_returnsFalse(status: OfferStatus) {
        // Arrange
        val offer = createOffer(status = status)
        val now = Instant.parse("2026-08-20T11:00:00Z")

        `when`(offerDBPort.findById(offer.id)).thenReturn(offer)

        // Act
        val result =
            processor.closeIfExpired(
                offerId = offer.id,
                now = now,
            )

        // Assert
        assertThat(result).isFalse()
        assertThat(offer.status).isEqualTo(status)
        assertThat(offer.updatedAt).isEqualTo(Instant.parse("2026-08-20T10:00:00Z"))

        verify(offerDBPort).findById(offer.id)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(offerDBPort)
    }

    private fun createOffer(
        id: OfferId = OfferId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        foodBagId: FoodBagId = FoodBagId(UUID.randomUUID()),
        status: OfferStatus = OfferStatus.ACTIVE,
        pickupWindow: PickupWindow =
            PickupWindow(
                start = Instant.parse("2026-08-20T09:00:00Z"),
                end = Instant.parse("2026-08-20T10:30:00Z"),
            ),
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        version: Long = 0,
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
            status = status,
            totalQuantity = 5,
            availableQuantity = 5,
            pickupWindow = pickupWindow,
            createdAt = Instant.parse("2026-08-20T09:00:00Z"),
            updatedAt = updatedAt,
            version = version,
        )
}
