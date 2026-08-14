package com.example.foodrescue.partnerservice.application.access

import com.example.foodrescue.partnerservice.application.exceptions.PartnerAccessDeniedException
import com.example.foodrescue.partnerservice.application.ports.CurrentUserPort
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreStaffDBPort
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import com.example.foodrescue.partnerservice.domain.enum.ApplicationRole
import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class PartnerAccessPolicyTest {

    private val partnerDBPort = mock(PartnerDBPort::class.java)
    private val storeStaffDBPort = mock(StoreStaffDBPort::class.java)
    private val currentUserPort = mock(CurrentUserPort::class.java)

    private val partnerAccessPolicy =
        PartnerAccessPolicy(
            storeStaffDBPort = storeStaffDBPort,
            currentUserPort = currentUserPort,
        )

    private val partnerId = PartnerId(UUID.randomUUID())

    @Test
    fun whenUserIsAdminAllowsAccessForReadAction() {
        val partner = createPartner()

        givenCurrentUser(
            userId = "admin-1",
            roles = setOf(ApplicationRole.ADMIN),
        )

        assertDoesNotThrow {
            partnerAccessPolicy.checkAccess(
                action = AccessAction.READ,
                resource = partner,
            )
        }

        verifyNoInteractions(
            partnerDBPort,
            storeStaffDBPort,
        )
    }

    @Test
    fun whenUserIsAnotherPartnerManagerThrowsPartnerAccessDeniedException() {
        val partner = createPartner(managerId = "another-manager")

        givenCurrentUser(
            userId = "manager-1",
            roles = setOf(ApplicationRole.MANAGER),
        )

        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)

        assertThrows<PartnerAccessDeniedException> {
            partnerAccessPolicy.checkAccess(
                action = AccessAction.READ,
                resource = partner,
            )
        }
    }

    @Test
    fun whenStaffIsAssignedToPartnerAllowsReadAccess() {
        val staffId = "staff-1"
        val partner = createPartner()

        givenCurrentUser(
            userId = staffId,
            roles = setOf(ApplicationRole.STAFF),
        )

        `when`(
                storeStaffDBPort.isStaffAssignedToAnyStoreOfPartner(
                    userId = staffId,
                    partnerId = partnerId,
                )
            )
            .thenReturn(true)

        assertDoesNotThrow {
            partnerAccessPolicy.checkAccess(
                action = AccessAction.READ,
                resource = partner,
            )
        }

        verify(storeStaffDBPort)
            .isStaffAssignedToAnyStoreOfPartner(
                userId = staffId,
                partnerId = partnerId,
            )
    }

    @Test
    fun whenStaffIsNotAssignedToPartnerThrowsPartnerAccessDeniedException() {
        val staffId = "staff-1"
        val partner = createPartner()

        givenCurrentUser(
            userId = staffId,
            roles = setOf(ApplicationRole.STAFF),
        )

        `when`(
                storeStaffDBPort.isStaffAssignedToAnyStoreOfPartner(
                    userId = staffId,
                    partnerId = partnerId,
                )
            )
            .thenReturn(false)

        assertThrows<PartnerAccessDeniedException> {
            partnerAccessPolicy.checkAccess(
                action = AccessAction.READ,
                resource = partner,
            )
        }
    }

    @Test
    fun whenUserIsPartnerManagerAllowsAccessForCreateOrUpdateAction() {
        val managerId = "manager-1"
        val partner = createPartner(managerId = managerId)

        givenCurrentUser(
            userId = managerId,
            roles = setOf(ApplicationRole.MANAGER),
        )

        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)

        assertDoesNotThrow {
            partnerAccessPolicy.checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = partner,
            )
        }
    }

    @Test
    fun whenUserIsPartnerStaffAllowsAccessForReadAction() {
        val partner = createPartner()

        givenCurrentUser(
            userId = "staff-1",
            roles = setOf(ApplicationRole.STAFF),
        )

        assertThrows<PartnerAccessDeniedException> {
            partnerAccessPolicy.checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = partner,
            )
        }

        verifyNoInteractions(
            partnerDBPort,
            storeStaffDBPort,
        )
    }

    private fun givenCurrentUser(
        userId: String,
        roles: Set<ApplicationRole>,
    ) {
        `when`(currentUserPort.getUserId()).thenReturn(userId)

        ApplicationRole.entries.forEach { role ->
            `when`(currentUserPort.hasRole(role)).thenReturn(role in roles)
        }
    }

    private fun createPartner(
        id: PartnerId = partnerId,
        managerId: String = "manager-1",
    ): Partner {
        val createdAt = Instant.parse("2026-01-01T10:00:00Z")

        return Partner(
            id = id,
            managerId = managerId,
            name = "Test partner",
            status = PartnerStatus.entries.first(),
            createdAt = createdAt,
            updatedAt = createdAt,
            version = 0,
        )
    }
}
