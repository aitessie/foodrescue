package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.access.FoodBagAccessPolicy
import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.offerservice.application.exceptions.FoodBagNotFoundException
import com.example.foodrescue.offerservice.application.exceptions.InvalidStateException
import com.example.foodrescue.offerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.offerservice.application.ports.FoodBagDBPort
import com.example.foodrescue.offerservice.domain.entities.FoodBag
import com.example.foodrescue.offerservice.domain.entities.FoodBagId
import com.example.foodrescue.offerservice.domain.entities.Money
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagCategory
import com.example.foodrescue.offerservice.domain.`enum`.FoodBagStatus
import com.example.foodrescue.offerservice.domain.`enum`.MoneyCurrency
import java.time.Clock
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
class CreateOrUpdateFoodBagUseCaseTest {
    @Mock private lateinit var foodBagDBPort: FoodBagDBPort

    @Mock private lateinit var foodBagAccessPolicy: FoodBagAccessPolicy

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: CreateOrUpdateFoodBagUseCase

    @Test
    fun whenNewFoodBagIsCreated_returnsSavedFoodBag() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val source = createFoodBag()
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedFoodBag =
            createFoodBag(
                id = source.id,
                storeId = source.storeId,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .foodBagCreated(
                    foodBag = savedFoodBag,
                    occurredAt = now,
                )

        `when`(foodBagDBPort.findById(source.id)).thenReturn(null)
        `when`(clock.instant()).thenReturn(now)
        `when`(foodBagDBPort.save(source)).thenReturn(savedFoodBag)
        `when`(
                eventFactory.foodBagCreated(
                    foodBag = savedFoodBag,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                source = source,
            )

        // Assert
        assertThat(result).isSameAs(savedFoodBag)

        verify(foodBagDBPort).findById(source.id)
        verify(foodBagAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = source.storeId,
            )
        verify(clock).instant()
        verify(foodBagDBPort).save(source)
        verify(eventFactory)
            .foodBagCreated(
                foodBag = savedFoodBag,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            foodBagDBPort,
            foodBagAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenExistingFoodBagIsUpdated_returnsSavedFoodBag() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val existing = createFoodBag()
        val source =
            createFoodBag(
                id = existing.id,
                storeId = existing.storeId,
                name = "Updated surprise bag",
                description = "Updated description",
                originalPrice =
                    Money(
                        amountMinor = 1200,
                        currency = MoneyCurrency.RUB,
                    ),
                unitPrice =
                    Money(
                        amountMinor = 600,
                        currency = MoneyCurrency.RUB,
                    ),
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedFoodBag =
            createFoodBag(
                id = existing.id,
                storeId = existing.storeId,
                name = source.name,
                description = source.description,
                originalPrice = source.originalPrice,
                unitPrice = source.unitPrice,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .foodBagUpdated(
                    foodBag = savedFoodBag,
                    occurredAt = now,
                )

        `when`(foodBagDBPort.findById(source.id)).thenReturn(existing)
        `when`(clock.instant()).thenReturn(now)
        `when`(foodBagDBPort.save(existing)).thenReturn(savedFoodBag)
        `when`(
                eventFactory.foodBagUpdated(
                    foodBag = savedFoodBag,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                source = source,
            )

        // Assert
        assertThat(result).isSameAs(savedFoodBag)
        assertThat(existing.name).isEqualTo(source.name)
        assertThat(existing.description).isEqualTo(source.description)
        assertThat(existing.originalPrice).isEqualTo(source.originalPrice)
        assertThat(existing.unitPrice).isEqualTo(source.unitPrice)
        assertThat(existing.updatedAt).isEqualTo(now)

        verify(foodBagDBPort).findById(source.id)
        verify(foodBagAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = source.storeId,
            )
        verify(clock).instant()
        verify(foodBagDBPort).save(existing)
        verify(eventFactory)
            .foodBagUpdated(
                foodBag = savedFoodBag,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            foodBagDBPort,
            foodBagAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenExistingFoodBagBelongsToAnotherStore_throwsFoodBagNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val existing = createFoodBag()
        val source =
            createFoodBag(
                id = existing.id,
                storeId = StoreId(UUID.randomUUID()),
            )

        `when`(foodBagDBPort.findById(source.id)).thenReturn(existing)

        // Act
        val exception =
            assertThrows<FoodBagNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    source = source,
                )
            }

        // Assert
        assertThat(exception.message).contains(source.id.value.toString())

        verify(foodBagDBPort).findById(source.id)
        verifyNoInteractions(
            foodBagAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(foodBagDBPort)
    }

    @Test
    fun whenUserCannotManageStore_throwsAccessDeniedException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val source = createFoodBag()
        val expectedException = AccessDeniedException()

        `when`(foodBagDBPort.findById(source.id)).thenReturn(null)
        doThrow(expectedException)
            .`when`(foodBagAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = source.storeId,
            )

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                useCase.execute(
                    partnerId = partnerId,
                    source = source,
                )
            }

        // Assert
        assertThat(exception).isSameAs(expectedException)

        verify(foodBagDBPort).findById(source.id)
        verify(foodBagAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = source.storeId,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            foodBagDBPort,
            foodBagAccessPolicy,
        )
    }

    @Test
    fun whenNewFoodBagHasNonZeroVersion_throwsEntityVersionConflictException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val source = createFoodBag(version = 1)

        `when`(foodBagDBPort.findById(source.id)).thenReturn(null)

        // Act
        val exception =
            assertThrows<EntityVersionConflictException> {
                useCase.execute(
                    partnerId = partnerId,
                    source = source,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo(
                "Version conflict for FoodBag '${source.id.value}': " +
                    "supplied version ${source.version} does not match current version 0"
            )

        verify(foodBagDBPort).findById(source.id)
        verify(foodBagAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = source.storeId,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            foodBagDBPort,
            foodBagAccessPolicy,
        )
    }

    @Test
    fun whenNewFoodBagIsNotActive_throwsInvalidStateException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val source = createFoodBag(status = FoodBagStatus.INACTIVE)

        `when`(foodBagDBPort.findById(source.id)).thenReturn(null)

        // Act
        val exception =
            assertThrows<InvalidStateException> {
                useCase.execute(
                    partnerId = partnerId,
                    source = source,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo("New FoodBag must have ACTIVE status")

        verify(foodBagDBPort).findById(source.id)
        verify(foodBagAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = source.storeId,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            foodBagDBPort,
            foodBagAccessPolicy,
        )
    }

    @Test
    fun whenUpdatedFoodBagVersionDoesNotMatchCurrentVersion_throwsEntityVersionConflictException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val existing = createFoodBag(version = 2)
        val source =
            createFoodBag(
                id = existing.id,
                storeId = existing.storeId,
                version = 1,
            )

        `when`(foodBagDBPort.findById(source.id)).thenReturn(existing)

        // Act
        val exception =
            assertThrows<EntityVersionConflictException> {
                useCase.execute(
                    partnerId = partnerId,
                    source = source,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo(
                "Version conflict for FoodBag '${existing.id.value}': " +
                    "supplied version ${source.version} does not match current version ${existing.version}"
            )

        verify(foodBagDBPort).findById(source.id)
        verify(foodBagAccessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = source.storeId,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            foodBagDBPort,
            foodBagAccessPolicy,
        )
    }

    private fun createFoodBag(
        id: FoodBagId = FoodBagId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        name: String = "Surprise bag",
        description: String? = "Food bag description",
        originalPrice: Money =
            Money(
                amountMinor = 1000,
                currency = MoneyCurrency.RUB,
            ),
        unitPrice: Money =
            Money(
                amountMinor = 500,
                currency = MoneyCurrency.RUB,
            ),
        status: FoodBagStatus = FoodBagStatus.ACTIVE,
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        version: Long = 0,
    ): FoodBag =
        FoodBag(
            id = id,
            storeId = storeId,
            name = name,
            description = description,
            category = FoodBagCategory.entries.first(),
            originalPrice = originalPrice,
            unitPrice = unitPrice,
            allergens = emptySet(),
            status = status,
            createdAt = Instant.parse("2026-08-20T10:00:00Z"),
            updatedAt = updatedAt,
            version = version,
        )
}
