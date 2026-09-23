package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.PartnerAccessPolicy
import com.example.foodrescue.partnerservice.application.events.ApplicationEventFactory
import com.example.foodrescue.partnerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.partnerservice.application.exceptions.PartnerAccessDeniedException
import com.example.foodrescue.partnerservice.application.exceptions.PartnerManagerChangeNotAllowedException
import com.example.foodrescue.partnerservice.application.ports.DomainEventPublisherPort
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
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
class CreateOrUpdatePartnerUseCaseTest {
    @Mock private lateinit var partnerDBPort: PartnerDBPort

    @Mock private lateinit var partnerAccessPolicy: PartnerAccessPolicy

    @Mock private lateinit var eventFactory: ApplicationEventFactory

    @Mock private lateinit var eventPublisherPort: DomainEventPublisherPort

    @Mock private lateinit var clock: Clock

    @InjectMocks private lateinit var useCase: CreateOrUpdatePartnerUseCase

    @Test
    fun whenNewPartnerIsCreated_returnsSavedPartner() {
        // Arrange
        val source = createPartner()
        val savedPartner =
            createPartner(
                id = source.id,
                managerId = source.managerId,
                name = source.name,
                status = source.status,
                createdAt = source.createdAt,
                updatedAt = source.updatedAt,
                version = 1,
            )

        `when`(partnerDBPort.findById(source.id)).thenReturn(null)
        `when`(partnerDBPort.save(source)).thenReturn(savedPartner)

        // Act
        val result = useCase.createOrUpdatePartner(source)

        // Assert
        assertThat(result).isSameAs(savedPartner)

        verify(partnerDBPort).findById(source.id)
        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = source,
            )
        verify(partnerDBPort).save(source)
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            partnerDBPort,
            partnerAccessPolicy,
        )
    }

    @Test
    fun whenCreatingPartnerAndAccessIsDenied_throwsPartnerAccessDeniedException() {
        // Arrange
        val source = createPartner()

        `when`(partnerDBPort.findById(source.id)).thenReturn(null)
        doThrow(PartnerAccessDeniedException())
            .`when`(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = source,
            )

        // Act
        val exception =
            assertThrows<PartnerAccessDeniedException> {
                useCase.createOrUpdatePartner(source)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user has no access to this partner")

        verify(partnerDBPort).findById(source.id)
        verify(partnerAccessPolicy)
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
            partnerDBPort,
            partnerAccessPolicy,
        )
    }

    @Test
    fun whenExistingActivePartnerIsUpdated_returnsSavedPartnerAndPublishesUpdatedEvent() {
        // Arrange
        val existingPartner =
            createPartner(
                name = "Old Partner",
                status = PartnerStatus.ACTIVE,
                version = 2,
            )
        val source =
            createPartner(
                id = existingPartner.id,
                managerId = existingPartner.managerId,
                name = "Updated Partner",
                status = PartnerStatus.ACTIVE,
                createdAt = existingPartner.createdAt,
                updatedAt = existingPartner.updatedAt,
                version = existingPartner.version,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedPartner =
            createPartner(
                id = existingPartner.id,
                managerId = existingPartner.managerId,
                name = source.name,
                status = PartnerStatus.ACTIVE,
                createdAt = existingPartner.createdAt,
                updatedAt = now,
                version = 3,
            )
        val event =
            ApplicationEventFactory()
                .partnerUpdated(
                    partner = savedPartner,
                    occurredAt = now,
                )

        `when`(partnerDBPort.findById(source.id)).thenReturn(existingPartner)
        `when`(clock.instant()).thenReturn(now)
        `when`(partnerDBPort.save(existingPartner)).thenReturn(savedPartner)
        `when`(
                eventFactory.partnerUpdated(
                    partner = savedPartner,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result = useCase.createOrUpdatePartner(source)

        // Assert
        assertThat(result).isSameAs(savedPartner)
        assertThat(existingPartner.name).isEqualTo(source.name)
        assertThat(existingPartner.status).isEqualTo(PartnerStatus.ACTIVE)
        assertThat(existingPartner.updatedAt).isEqualTo(now)

        verify(partnerDBPort).findById(source.id)
        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingPartner,
            )
        verify(clock).instant()
        verify(partnerDBPort).save(existingPartner)
        verify(eventFactory)
            .partnerUpdated(
                partner = savedPartner,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            partnerDBPort,
            partnerAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenExistingPartnerIsSuspended_returnsSavedPartnerAndPublishesSuspendedEvent() {
        // Arrange
        val existingPartner =
            createPartner(
                name = "Partner",
                status = PartnerStatus.ACTIVE,
                version = 2,
            )
        val source =
            createPartner(
                id = existingPartner.id,
                managerId = existingPartner.managerId,
                name = existingPartner.name,
                status = PartnerStatus.SUSPENDED,
                createdAt = existingPartner.createdAt,
                updatedAt = existingPartner.updatedAt,
                version = existingPartner.version,
            )
        val now = Instant.parse("2026-08-20T11:00:00Z")
        val savedPartner =
            createPartner(
                id = existingPartner.id,
                managerId = existingPartner.managerId,
                name = source.name,
                status = PartnerStatus.SUSPENDED,
                createdAt = existingPartner.createdAt,
                updatedAt = now,
                version = 3,
            )
        val event =
            ApplicationEventFactory()
                .partnerSuspended(
                    partner = savedPartner,
                    occurredAt = now,
                )

        `when`(partnerDBPort.findById(source.id)).thenReturn(existingPartner)
        `when`(clock.instant()).thenReturn(now)
        `when`(partnerDBPort.save(existingPartner)).thenReturn(savedPartner)
        `when`(
                eventFactory.partnerSuspended(
                    partner = savedPartner,
                    occurredAt = now,
                )
            )
            .thenReturn(event)

        // Act
        val result = useCase.createOrUpdatePartner(source)

        // Assert
        assertThat(result).isSameAs(savedPartner)
        assertThat(existingPartner.status).isEqualTo(PartnerStatus.SUSPENDED)
        assertThat(existingPartner.updatedAt).isEqualTo(now)

        verify(partnerDBPort).findById(source.id)
        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingPartner,
            )
        verify(clock).instant()
        verify(partnerDBPort).save(existingPartner)
        verify(eventFactory)
            .partnerSuspended(
                partner = savedPartner,
                occurredAt = now,
            )
        verify(eventPublisherPort).publish(event)
        verifyNoMoreInteractions(
            partnerDBPort,
            partnerAccessPolicy,
            eventFactory,
            eventPublisherPort,
            clock,
        )
    }

    @Test
    fun whenUpdatingPartnerAndAccessIsDenied_throwsPartnerAccessDeniedException() {
        // Arrange
        val existingPartner = createPartner(version = 2)
        val source =
            createPartner(
                id = existingPartner.id,
                managerId = existingPartner.managerId,
                version = existingPartner.version,
            )

        `when`(partnerDBPort.findById(source.id)).thenReturn(existingPartner)
        doThrow(PartnerAccessDeniedException())
            .`when`(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingPartner,
            )

        // Act
        val exception =
            assertThrows<PartnerAccessDeniedException> {
                useCase.createOrUpdatePartner(source)
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user has no access to this partner")

        verify(partnerDBPort).findById(source.id)
        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingPartner,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            partnerDBPort,
            partnerAccessPolicy,
        )
    }

    @Test
    fun whenUpdatedPartnerHasDifferentManager_throwsPartnerManagerChangeNotAllowedException() {
        // Arrange
        val existingPartner =
            createPartner(
                managerId = MANAGER_ID,
                version = 2,
            )
        val source =
            createPartner(
                id = existingPartner.id,
                managerId = OTHER_MANAGER_ID,
                version = existingPartner.version,
            )

        `when`(partnerDBPort.findById(source.id)).thenReturn(existingPartner)

        // Act
        val exception =
            assertThrows<PartnerManagerChangeNotAllowedException> {
                useCase.createOrUpdatePartner(source)
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo("Partner ${existingPartner.id.value} manager cannot be changed")
        assertThat(exception.partnerId).isEqualTo(existingPartner.id)

        verify(partnerDBPort).findById(source.id)
        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingPartner,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            partnerDBPort,
            partnerAccessPolicy,
        )
    }

    @Test
    fun whenUpdatedPartnerVersionDoesNotMatchCurrentVersion_throwsEntityVersionConflictException() {
        // Arrange
        val existingPartner = createPartner(version = 2)
        val source =
            createPartner(
                id = existingPartner.id,
                managerId = existingPartner.managerId,
                version = 1,
            )

        `when`(partnerDBPort.findById(source.id)).thenReturn(existingPartner)

        // Act
        val exception =
            assertThrows<EntityVersionConflictException> {
                useCase.createOrUpdatePartner(source)
            }

        // Assert
        assertThat(exception.message)
            .isEqualTo(
                "Partner ${existingPartner.id.value} version conflict: " +
                    "expected=${source.version}, actual=${existingPartner.version}"
            )
        assertThat(exception.entityType).isEqualTo("Partner")
        assertThat(exception.entityId).isEqualTo(existingPartner.id.value.toString())
        assertThat(exception.expectedVersion).isEqualTo(source.version)
        assertThat(exception.actualVersion).isEqualTo(existingPartner.version)

        verify(partnerDBPort).findById(source.id)
        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingPartner,
            )
        verifyNoInteractions(
            eventFactory,
            eventPublisherPort,
            clock,
        )
        verifyNoMoreInteractions(
            partnerDBPort,
            partnerAccessPolicy,
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

    companion object {
        private const val MANAGER_ID = "33333333-3333-3333-3333-333333333333"
        private const val OTHER_MANAGER_ID = "88888888-8888-8888-8888-888888888888"
    }
}
