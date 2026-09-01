package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.access.FoodBagAccessPolicy
import com.example.foodrescue.offerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.offerservice.application.exceptions.FoodBagNotFoundException
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
class ChangeFoodBagStatusUseCaseTest {
    @Mock private lateinit var foodBagDBPort: FoodBagDBPort

    @Mock private lateinit var accessPolicy: FoodBagAccessPolicy

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: ChangeFoodBagStatusUseCase

    @Test
    fun whenFoodBagStatusIsChanged_returnsSavedFoodBag() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val foodBag = createFoodBag(storeId = storeId)
        val now = Instant.parse("2026-08-21T10:00:00Z")
        val savedFoodBag =
            createFoodBag(
                id = foodBag.id,
                storeId = storeId,
                status = FoodBagStatus.ACTIVE,
                updatedAt = now,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .foodBagStatusChanged(
                    foodBag = savedFoodBag,
                    occurredAt = now,
                )

        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)
        `when`(clock.instant()).thenReturn(now)
        `when`(foodBagDBPort.save(foodBag)).thenReturn(savedFoodBag)
        `when`(
                eventFactory.foodBagStatusChanged(
                    foodBag = savedFoodBag,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                storeId = storeId,
                foodBagId = foodBag.id,
                targetStatus = FoodBagStatus.ACTIVE,
                expectedVersion = 0,
            )

        // Assert
        assertThat(result).isSameAs(savedFoodBag)
        assertThat(foodBag.status).isEqualTo(FoodBagStatus.ACTIVE)
        assertThat(foodBag.updatedAt).isEqualTo(now)

        verify(foodBagDBPort).findById(foodBag.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(clock).instant()
        verify(foodBagDBPort).save(foodBag)
        verify(eventFactory)
            .foodBagStatusChanged(
                foodBag = savedFoodBag,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            foodBagDBPort,
            accessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenTargetStatusMatchesCurrentStatus_returnsExistingFoodBag() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val foodBag =
            createFoodBag(
                storeId = storeId,
                status = FoodBagStatus.ACTIVE,
            )
        val originalUpdatedAt = foodBag.updatedAt
        val now = Instant.parse("2026-08-21T10:00:00Z")

        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)
        `when`(clock.instant()).thenReturn(now)

        // Act
        val result =
            useCase.execute(
                partnerId = partnerId,
                storeId = storeId,
                foodBagId = foodBag.id,
                targetStatus = FoodBagStatus.ACTIVE,
                expectedVersion = 0,
            )

        // Assert
        assertThat(result).isSameAs(foodBag)
        assertThat(foodBag.status).isEqualTo(FoodBagStatus.ACTIVE)
        assertThat(foodBag.updatedAt).isEqualTo(originalUpdatedAt)

        verify(foodBagDBPort).findById(foodBag.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verify(clock).instant()
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            foodBagDBPort,
            accessPolicy,
            clock,
        )
    }

    @Test
    fun whenFoodBagDoesNotExist_throwsFoodBagNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val foodBagId = FoodBagId(UUID.randomUUID())

        `when`(foodBagDBPort.findById(foodBagId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<FoodBagNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    foodBagId = foodBagId,
                    targetStatus = FoodBagStatus.ACTIVE,
                    expectedVersion = 0,
                )
            }

        // Assert
        assertThat(exception.message).contains(foodBagId.value.toString())

        verify(foodBagDBPort).findById(foodBagId)
        verifyNoInteractions(
            accessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(foodBagDBPort)
    }

    @Test
    fun whenFoodBagBelongsToAnotherStore_throwsFoodBagNotFoundException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val requestedStoreId = StoreId(UUID.randomUUID())
        val foodBag = createFoodBag()

        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)

        // Act
        val exception =
            assertThrows<FoodBagNotFoundException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = requestedStoreId,
                    foodBagId = foodBag.id,
                    targetStatus = FoodBagStatus.ACTIVE,
                    expectedVersion = foodBag.version,
                )
            }

        // Assert
        assertThat(exception.message).contains(foodBag.id.value.toString())

        verify(foodBagDBPort).findById(foodBag.id)
        verifyNoInteractions(
            accessPolicy,
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
        val storeId = StoreId(UUID.randomUUID())
        val foodBag = createFoodBag(storeId = storeId)
        val expectedMessage = "Current user cannot manage Store ${storeId.value}"

        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)
        doThrow(AccessDeniedException(expectedMessage))
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
                    foodBagId = foodBag.id,
                    targetStatus = FoodBagStatus.ACTIVE,
                    expectedVersion = foodBag.version,
                )
            }

        // Assert
        assertThat(exception.message).isEqualTo(expectedMessage)

        verify(foodBagDBPort).findById(foodBag.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            foodBagDBPort,
            accessPolicy,
        )
    }

    @Test
    fun whenExpectedVersionDoesNotMatchCurrentVersion_throwsEntityVersionConflictException() {
        // Arrange
        val partnerId = PartnerId(UUID.randomUUID())
        val storeId = StoreId(UUID.randomUUID())
        val foodBag =
            createFoodBag(
                storeId = storeId,
                version = 2,
            )
        val expectedVersion = 1L

        `when`(foodBagDBPort.findById(foodBag.id)).thenReturn(foodBag)

        // Act
        val exception =
            assertThrows<EntityVersionConflictException> {
                useCase.execute(
                    partnerId = partnerId,
                    storeId = storeId,
                    foodBagId = foodBag.id,
                    targetStatus = FoodBagStatus.ACTIVE,
                    expectedVersion = expectedVersion,
                )
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo(
                "Version conflict for FoodBag '${foodBag.id.value}': " +
                    "supplied version $expectedVersion does not match current version ${foodBag.version}"
            )

        verify(foodBagDBPort).findById(foodBag.id)
        verify(accessPolicy)
            .checkAccess(
                partnerId = partnerId,
                storeId = storeId,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            foodBagDBPort,
            accessPolicy,
        )
    }

    private fun createFoodBag(
        id: FoodBagId = FoodBagId(UUID.randomUUID()),
        storeId: StoreId = StoreId(UUID.randomUUID()),
        status: FoodBagStatus = FoodBagStatus.INACTIVE,
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        version: Long = 0,
    ): FoodBag =
        FoodBag(
            id = id,
            storeId = storeId,
            name = "Surprise bag",
            description = "Food bag description",
            category = FoodBagCategory.entries.first(),
            originalPrice =
                Money(
                    amountMinor = 1000,
                    currency = MoneyCurrency.RUB,
                ),
            unitPrice =
                Money(
                    amountMinor = 500,
                    currency = MoneyCurrency.RUB,
                ),
            allergens = emptySet(),
            status = status,
            createdAt = Instant.parse("2026-08-20T10:00:00Z"),
            updatedAt = updatedAt,
            version = version,
        )
}
