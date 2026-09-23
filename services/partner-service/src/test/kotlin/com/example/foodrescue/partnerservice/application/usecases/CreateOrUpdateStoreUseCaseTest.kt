package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.StoreAccessPolicy
import com.example.foodrescue.partnerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.partnerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.partnerservice.application.exceptions.PartnerNotFoundException
import com.example.foodrescue.partnerservice.application.exceptions.StoreAccessDeniedException
import com.example.foodrescue.partnerservice.application.exceptions.StoreNotFoundException
import com.example.foodrescue.partnerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreDBPort
import com.example.foodrescue.partnerservice.domain.entities.Address
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.entities.Store
import com.example.foodrescue.partnerservice.domain.entities.StoreId
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
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
class CreateOrUpdateStoreUseCaseTest {
    @Mock private lateinit var storeDBPort: StoreDBPort

    @Mock private lateinit var partnerDBPort: PartnerDBPort

    @Mock private lateinit var storeAccessPolicy: StoreAccessPolicy

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: CreateOrUpdateStoreUseCase

    @Test
    fun whenNewStoreIsCreated_returnsSavedStoreAndPublishesCreatedEvent() {
        // Arrange
        val partner = createPartner()
        val source = createStore(partnerId = partner.id)
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedStore =
            createStore(
                id = source.id,
                partnerId = source.partnerId,
                name = source.name,
                status = source.status,
                address = source.address,
                createdAt = source.createdAt,
                updatedAt = source.updatedAt,
                version = 1,
            )
        val event =
            ApplicationEventFactory()
                .storeCreated(
                    store = savedStore,
                    partner = partner,
                    occurredAt = now,
                )

        `when`(storeDBPort.findById(source.id)).thenReturn(null)
        `when`(partnerDBPort.findById(source.partnerId)).thenReturn(partner)
        `when`(storeDBPort.save(source)).thenReturn(savedStore)
        `when`(clock.instant()).thenReturn(now)
        `when`(
                eventFactory.storeCreated(
                    store = savedStore,
                    partner = partner,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result = useCase.createOrUpdateStore(source)

        // Assert
        assertThat(result).isSameAs(savedStore)

        verify(storeDBPort).findById(source.id)
        verify(partnerDBPort).findById(source.partnerId)
        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = source,
            )
        verify(storeDBPort).save(source)
        verify(clock).instant()
        verify(eventFactory)
            .storeCreated(
                store = savedStore,
                partner = partner,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            storeDBPort,
            partnerDBPort,
            storeAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenCreatingStoreAndPartnerDoesNotExist_throwsPartnerNotFoundException() {
        // Arrange
        val source = createStore()

        `when`(storeDBPort.findById(source.id)).thenReturn(null)
        `when`(partnerDBPort.findById(source.partnerId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<PartnerNotFoundException> {
                useCase.createOrUpdateStore(source)
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Partner with id ${source.partnerId.value} was not found")

        verify(storeDBPort).findById(source.id)
        verify(partnerDBPort).findById(source.partnerId)
        verifyNoInteractions(
            storeAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            storeDBPort,
            partnerDBPort,
        )
    }

    @Test
    fun whenCreatingStoreAndAccessIsDenied_throwsStoreAccessDeniedException() {
        // Arrange
        val partner = createPartner()
        val source = createStore(partnerId = partner.id)

        `when`(storeDBPort.findById(source.id)).thenReturn(null)
        `when`(partnerDBPort.findById(source.partnerId)).thenReturn(partner)
        doThrow(StoreAccessDeniedException())
            .`when`(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = source,
            )

        // Act
        val exception =
            assertThrows<StoreAccessDeniedException> {
                useCase.createOrUpdateStore(source)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user has no access to this store")

        verify(storeDBPort).findById(source.id)
        verify(partnerDBPort).findById(source.partnerId)
        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = source,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            storeDBPort,
            partnerDBPort,
            storeAccessPolicy,
        )
    }

    @Test
    fun whenExistingActiveStoreIsUpdated_returnsSavedStoreAndPublishesUpdatedEvent() {
        // Arrange
        val partner = createPartner()
        val existingStore =
            createStore(
                partnerId = partner.id,
                name = "Old Store",
                status = StoreStatus.ACTIVE,
                address =
                    Address(
                        city = "Saint Petersburg",
                        street = "Old Street",
                        building = "1",
                        postalCode = "190000",
                    ),
                version = 2,
            )
        val sourceAddress =
            Address(
                city = "Saint Petersburg",
                street = "Nevsky Prospekt",
                building = "10",
                postalCode = "191025",
            )
        val source =
            createStore(
                id = existingStore.id,
                partnerId = existingStore.partnerId,
                name = "Updated Store",
                status = StoreStatus.ACTIVE,
                address = sourceAddress,
                createdAt = existingStore.createdAt,
                updatedAt = existingStore.updatedAt,
                version = existingStore.version,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedStore =
            createStore(
                id = existingStore.id,
                partnerId = existingStore.partnerId,
                name = source.name,
                status = source.status,
                address = sourceAddress,
                createdAt = existingStore.createdAt,
                updatedAt = now,
                version = 3,
            )
        val event =
            ApplicationEventFactory()
                .storeUpdated(
                    store = savedStore,
                    partner = partner,
                    occurredAt = now,
                )

        `when`(storeDBPort.findById(source.id)).thenReturn(existingStore)
        `when`(clock.instant()).thenReturn(now)
        `when`(storeDBPort.save(existingStore)).thenReturn(savedStore)
        `when`(partnerDBPort.findById(savedStore.partnerId)).thenReturn(partner)
        `when`(
                eventFactory.storeUpdated(
                    store = savedStore,
                    partner = partner,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result = useCase.createOrUpdateStore(source)

        // Assert
        assertThat(result).isSameAs(savedStore)
        assertThat(existingStore.name).isEqualTo(source.name)
        assertThat(existingStore.status).isEqualTo(StoreStatus.ACTIVE)
        assertThat(existingStore.address).isSameAs(sourceAddress)
        assertThat(existingStore.updatedAt).isEqualTo(now)

        verify(storeDBPort).findById(source.id)
        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingStore,
            )
        verify(clock).instant()
        verify(storeDBPort).save(existingStore)
        verify(partnerDBPort).findById(savedStore.partnerId)
        verify(eventFactory)
            .storeUpdated(
                store = savedStore,
                partner = partner,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            storeDBPort,
            partnerDBPort,
            storeAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenExistingStoreIsSuspended_returnsSavedStoreAndPublishesSuspendedEvent() {
        // Arrange
        val partner = createPartner()
        val existingStore =
            createStore(
                partnerId = partner.id,
                status = StoreStatus.ACTIVE,
                version = 2,
            )
        val source =
            createStore(
                id = existingStore.id,
                partnerId = existingStore.partnerId,
                name = existingStore.name,
                status = StoreStatus.SUSPENDED,
                address = existingStore.address,
                createdAt = existingStore.createdAt,
                updatedAt = existingStore.updatedAt,
                version = existingStore.version,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedStore =
            createStore(
                id = existingStore.id,
                partnerId = existingStore.partnerId,
                name = source.name,
                status = StoreStatus.SUSPENDED,
                address = source.address,
                createdAt = existingStore.createdAt,
                updatedAt = now,
                version = 3,
            )
        val event =
            ApplicationEventFactory()
                .storeSuspended(
                    store = savedStore,
                    partner = partner,
                    occurredAt = now,
                )

        `when`(storeDBPort.findById(source.id)).thenReturn(existingStore)
        `when`(clock.instant()).thenReturn(now)
        `when`(storeDBPort.save(existingStore)).thenReturn(savedStore)
        `when`(partnerDBPort.findById(savedStore.partnerId)).thenReturn(partner)
        `when`(
                eventFactory.storeSuspended(
                    store = savedStore,
                    partner = partner,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result = useCase.createOrUpdateStore(source)

        // Assert
        assertThat(result).isSameAs(savedStore)
        assertThat(existingStore.status).isEqualTo(StoreStatus.SUSPENDED)
        assertThat(existingStore.updatedAt).isEqualTo(now)

        verify(storeDBPort).findById(source.id)
        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingStore,
            )
        verify(clock).instant()
        verify(storeDBPort).save(existingStore)
        verify(partnerDBPort).findById(savedStore.partnerId)
        verify(eventFactory)
            .storeSuspended(
                store = savedStore,
                partner = partner,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            storeDBPort,
            partnerDBPort,
            storeAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenUpdatedStoreBelongsToAnotherPartner_throwsStoreNotFoundException() {
        // Arrange
        val existingStore = createStore()
        val source =
            createStore(
                id = existingStore.id,
                partnerId = PartnerId(UUID.randomUUID()),
                version = existingStore.version,
            )

        `when`(storeDBPort.findById(source.id)).thenReturn(existingStore)

        // Act
        val exception =
            assertThrows<StoreNotFoundException> {
                useCase.createOrUpdateStore(source)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Store with id ${source.id.value} was not found")

        verify(storeDBPort).findById(source.id)
        verifyNoInteractions(
            partnerDBPort,
            storeAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(storeDBPort)
    }

    @Test
    fun whenUpdatingStoreAndAccessIsDenied_throwsStoreAccessDeniedException() {
        // Arrange
        val existingStore = createStore(version = 2)
        val source =
            createStore(
                id = existingStore.id,
                partnerId = existingStore.partnerId,
                version = existingStore.version,
            )

        `when`(storeDBPort.findById(source.id)).thenReturn(existingStore)
        doThrow(StoreAccessDeniedException())
            .`when`(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingStore,
            )

        // Act
        val exception =
            assertThrows<StoreAccessDeniedException> {
                useCase.createOrUpdateStore(source)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user has no access to this store")

        verify(storeDBPort).findById(source.id)
        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingStore,
            )
        verifyNoInteractions(
            partnerDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            storeDBPort,
            storeAccessPolicy,
        )
    }

    @Test
    fun whenUpdatedStoreVersionDoesNotMatchCurrentVersion_throwsEntityVersionConflictException() {
        // Arrange
        val existingStore = createStore(version = 2)
        val source =
            createStore(
                id = existingStore.id,
                partnerId = existingStore.partnerId,
                version = 1,
            )

        `when`(storeDBPort.findById(source.id)).thenReturn(existingStore)

        // Act
        val exception =
            assertThrows<EntityVersionConflictException> {
                useCase.createOrUpdateStore(source)
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo(
                "Store ${existingStore.id.value} version conflict: " +
                    "expected=${source.version}, actual=${existingStore.version}"
            )
        assertThat(exception.entityType).isEqualTo("Store")
        assertThat(exception.entityId).isEqualTo(existingStore.id.value.toString())
        assertThat(exception.expectedVersion).isEqualTo(source.version)
        assertThat(exception.actualVersion).isEqualTo(existingStore.version)

        verify(storeDBPort).findById(source.id)
        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingStore,
            )
        verifyNoInteractions(
            partnerDBPort,
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            storeDBPort,
            storeAccessPolicy,
        )
    }

    @Test
    fun whenPartnerDoesNotExistAfterStoreUpdate_throwsPartnerNotFoundException() {
        // Arrange
        val existingStore = createStore(version = 2)
        val source =
            createStore(
                id = existingStore.id,
                partnerId = existingStore.partnerId,
                name = "Updated Store",
                version = existingStore.version,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedStore =
            createStore(
                id = existingStore.id,
                partnerId = existingStore.partnerId,
                name = source.name,
                status = source.status,
                address = source.address,
                createdAt = existingStore.createdAt,
                updatedAt = now,
                version = 3,
            )

        `when`(storeDBPort.findById(source.id)).thenReturn(existingStore)
        `when`(clock.instant()).thenReturn(now)
        `when`(storeDBPort.save(existingStore)).thenReturn(savedStore)
        `when`(partnerDBPort.findById(savedStore.partnerId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<PartnerNotFoundException> {
                useCase.createOrUpdateStore(source)
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Partner with id ${savedStore.partnerId.value} was not found")

        assertThat(existingStore.name).isEqualTo(source.name)
        assertThat(existingStore.updatedAt).isEqualTo(now)

        verify(storeDBPort).findById(source.id)
        verify(storeAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingStore,
            )
        verify(clock).instant()
        verify(storeDBPort).save(existingStore)
        verify(partnerDBPort).findById(savedStore.partnerId)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
        )
        verifyNoMoreInteractions(
            storeDBPort,
            partnerDBPort,
            storeAccessPolicy,
            clock,
        )
    }

    private fun createPartner(
        id: PartnerId = PartnerId(UUID.randomUUID()),
        managerId: String = MANAGER_ID,
        name: String = "Partner",
        status: PartnerStatus = PartnerStatus.ACTIVE,
        createdAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        version: Long = 0,
    ): Partner =
        Partner(
            id = id,
            managerId = managerId,
            name = name,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )

    private fun createStore(
        id: StoreId = StoreId(UUID.randomUUID()),
        partnerId: PartnerId = PartnerId(UUID.randomUUID()),
        name: String = "Store",
        status: StoreStatus = StoreStatus.ACTIVE,
        address: Address =
            Address(
                city = "Saint Petersburg",
                street = "Nevsky Prospekt",
                building = "1",
                postalCode = "190000",
            ),
        createdAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-08-20T10:00:00Z"),
        version: Long = 0,
    ): Store =
        Store(
            id = id,
            partnerId = partnerId,
            name = name,
            status = status,
            workingHours = emptyList(),
            address = address,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )

    companion object {
        private const val MANAGER_ID = "33333333-3333-3333-3333-333333333333"
    }
}
