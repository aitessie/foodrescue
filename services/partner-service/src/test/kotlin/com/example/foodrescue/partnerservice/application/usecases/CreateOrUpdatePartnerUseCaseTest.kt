package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.PartnerAccessPolicy
import com.example.foodrescue.partnerservice.application.exceptions.EntityVersionConflictException
import com.example.foodrescue.partnerservice.application.exceptions.PartnerAccessDeniedException
import com.example.foodrescue.partnerservice.application.exceptions.PartnerManagerChangeNotAllowedException
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*

class CreateOrUpdatePartnerUseCaseTest {

    private val partnerDBPort = mock(PartnerDBPort::class.java)
    private val partnerAccessPolicy = mock(PartnerAccessPolicy::class.java)

    private val updatedAt = Instant.parse("2026-01-02T10:00:00Z")

    private val clock =
        Clock.fixed(
            updatedAt,
            ZoneOffset.UTC,
        )

    private val createOrUpdatePartnerUseCase =
        CreateOrUpdatePartnerUseCase(
            partnerDBPort = partnerDBPort,
            partnerAccessPolicy = partnerAccessPolicy,
            clock = clock,
        )

    private val partnerId = PartnerId(UUID.randomUUID())

    @Test
    fun whenPartnerDoesNotExistCreatesPartner() {
        // arrange
        val source = createPartner()

        // act
        `when`(partnerDBPort.findById(partnerId)).thenReturn(null)

        `when`(partnerDBPort.save(source)).thenReturn(source)

        val result = createOrUpdatePartnerUseCase.createOrUpdatePartner(source)

        // assert
        assertThat(source).isEqualTo(result)

        verify(partnerDBPort).findById(partnerId)

        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = source,
            )

        verify(partnerDBPort).save(source)
    }

    @Test
    fun whenManagerIdIsChangedThrowsPartnerManagerChangeNotAllowedException() {
        // arrange
        val existingPartner =
            createPartner(
                managerId = "existing-manager-id",
                name = "Existing partner",
                version = 2,
            )

        val source =
            createPartner(
                managerId = "another-manager-id",
                name = "Updated partner",
                version = 2,
            )

        `when`(partnerDBPort.findById(partnerId)).thenReturn(existingPartner)

        // act
        val exception =
            assertThrows<PartnerManagerChangeNotAllowedException> {
                createOrUpdatePartnerUseCase.createOrUpdatePartner(source)
            }

        // assert
        assertThat(exception.partnerId).isEqualTo(partnerId)

        assertThat(existingPartner.managerId).isEqualTo("existing-manager-id")

        assertThat(existingPartner.name).isEqualTo("Existing partner")

        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingPartner,
            )

        verify(partnerDBPort, never()).save(existingPartner)

        verify(partnerDBPort, never()).save(source)
    }

    @Test
    fun whenVersionsMatchUpdatesExistingPartner() {
        // arrange
        val existingPartner =
            createPartner(
                name = "Old name",
                version = 3,
            )

        val source =
            createPartner(
                name = "Updated name",
                status = PartnerStatus.entries.last(),
                version = 3,
            )

        // act
        `when`(partnerDBPort.findById(partnerId)).thenReturn(existingPartner)
        `when`(partnerDBPort.save(existingPartner)).thenReturn(existingPartner)

        val result = createOrUpdatePartnerUseCase.createOrUpdatePartner(source)

        // assert
        assertThat(existingPartner).isEqualTo(result)
        assertThat(existingPartner).isEqualTo(result)
        assertThat("Updated name").isEqualTo(result.name)
        assertThat(source.status).isEqualTo(result.status)
        assertThat(updatedAt).isEqualTo(result.updatedAt)
        assertThat(3).isEqualTo(result.version)

        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingPartner,
            )

        verify(partnerDBPort).save(existingPartner)
    }

    @Test
    fun whenVersionsDifferThrowsEntityVersionConflictException() {
        // arrange
        val existingPartner =
            createPartner(
                name = "Existing name",
                version = 5,
            )

        val source =
            createPartner(
                name = "Updated name",
                version = 4,
            )

        // act
        `when`(partnerDBPort.findById(partnerId)).thenReturn(existingPartner)

        val exception =
            assertThrows<EntityVersionConflictException> {
                createOrUpdatePartnerUseCase.createOrUpdatePartner(source)
            }

        // assert
        assertThat(Partner::class.simpleName).isEqualTo(exception.entityType)
        assertThat(partnerId.value.toString()).isEqualTo(exception.entityId)
        assertThat(4).isEqualTo(exception.expectedVersion)
        assertThat(5).isEqualTo(exception.actualVersion)
        assertThat("Existing name").isEqualTo(existingPartner.name)

        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingPartner,
            )

        verify(partnerDBPort, never()).save(existingPartner)
    }

    @Test
    fun whenAccessIsDeniedDoNotUpdatePartner() {
        // arrange
        val existingPartner =
            createPartner(
                name = "Existing name",
                version = 2,
            )

        val source =
            createPartner(
                name = "Updated name",
                version = 2,
            )

        // act
        `when`(partnerDBPort.findById(partnerId)).thenReturn(existingPartner)

        doThrow(PartnerAccessDeniedException())
            .`when`(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = existingPartner,
            )

        // assert
        assertThrows<PartnerAccessDeniedException> {
            createOrUpdatePartnerUseCase.createOrUpdatePartner(source)
        }

        assertEquals("Existing name", existingPartner.name)

        verify(partnerDBPort, never()).save(existingPartner)
    }

    private fun createPartner(
        id: PartnerId = partnerId,
        managerId: String = "manager-1",
        name: String = "Test partner",
        status: PartnerStatus = PartnerStatus.ACTIVE,
        version: Long = 0,
    ): Partner {
        val createdAt = Instant.parse("2026-01-01T10:00:00Z")

        return Partner(
            id = id,
            managerId = managerId,
            name = name,
            status = status,
            createdAt = createdAt,
            updatedAt = createdAt,
            version = version,
        )
    }
}
