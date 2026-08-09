package com.example.foodrescue.partnerservice.application.access

import com.example.foodrescue.partnerservice.application.exception.StoreAccessDeniedException
import com.example.foodrescue.partnerservice.application.ports.CurrentUserPort
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.application.ports.StoreStaffDBPort
import com.example.foodrescue.partnerservice.domain.entity.Address
import com.example.foodrescue.partnerservice.domain.entity.Partner
import com.example.foodrescue.partnerservice.domain.entity.PartnerId
import com.example.foodrescue.partnerservice.domain.entity.Store
import com.example.foodrescue.partnerservice.domain.entity.StoreId
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import com.example.foodrescue.partnerservice.domain.enum.ApplicationRole
import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import com.example.foodrescue.partnerservice.domain.enum.StoreStatus
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class StoreAccessPolicyTest {

    private val partnerDBPort = mock(PartnerDBPort::class.java)
    private val storeStaffDBPort = mock(StoreStaffDBPort::class.java)
    private val currentUserPort = mock(CurrentUserPort::class.java)

    private val storeAccessPolicy =
        StoreAccessPolicy(
            partnerDBPort = partnerDBPort,
            storeStaffDBPort = storeStaffDBPort,
            currentUserPort = currentUserPort,
        )

    private val storeId = StoreId(UUID.randomUUID())

    private val partnerId = PartnerId(UUID.randomUUID())

    @Test
    fun whenUserIsAdminAllowsAccessForReadAction() {
        val store = createStore()

        givenCurrentUser(
            userId = "admin-1",
            roles = setOf(ApplicationRole.ADMIN),
        )

        assertDoesNotThrow {
            storeAccessPolicy.checkAccess(
                action = AccessAction.READ,
                resource = store,
            )
        }

        verifyNoInteractions(
            partnerDBPort,
            storeStaffDBPort,
        )
    }

    @Test
    fun whenUserIsPartnerManagerAllowsAccessForReadAction() {
        val managerId = "manager-1"
        val store = createStore()
        val partner = createPartner(managerId = managerId)

        givenCurrentUser(
            userId = managerId,
            roles = setOf(ApplicationRole.MANAGER),
        )

        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)

        assertDoesNotThrow {
            storeAccessPolicy.checkAccess(
                action = AccessAction.READ,
                resource = store,
            )
        }

        verify(partnerDBPort).findById(partnerId)
    }

    @Test
    fun whenUserIsAnotherPartnerManagerThrowsStoreAccessDeniedException() {
        val store = createStore()
        val partner = createPartner(managerId = "another-manager")

        givenCurrentUser(
            userId = "manager-1",
            roles = setOf(ApplicationRole.MANAGER),
        )

        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)

        assertThrows<StoreAccessDeniedException> {
            storeAccessPolicy.checkAccess(
                action = AccessAction.READ,
                resource = store,
            )
        }
    }

    @Test
    fun whenStaffIsAssignedToStoreAllowsReadAccess() {
        val staffId = "staff-1"
        val store = createStore()

        givenCurrentUser(
            userId = staffId,
            roles = setOf(ApplicationRole.STAFF),
        )

        `when`(storeStaffDBPort.isStaffAssignedToStore(userId = staffId, storeId = storeId))
            .thenReturn(true)

        assertDoesNotThrow {
            storeAccessPolicy.checkAccess(
                action = AccessAction.READ,
                resource = store,
            )
        }

        verify(storeStaffDBPort)
            .isStaffAssignedToStore(
                userId = staffId,
                storeId = storeId,
            )
    }

    @Test
    fun whenStaffIsNotAssignedToStoreThrowsStoreAccessDeniedException() {
        val staffId = "staff-1"
        val store = createStore()

        givenCurrentUser(
            userId = staffId,
            roles = setOf(ApplicationRole.STAFF),
        )

        `when`(
                storeStaffDBPort.isStaffAssignedToStore(
                    userId = staffId,
                    storeId = storeId,
                )
            )
            .thenReturn(false)

        assertThrows<StoreAccessDeniedException> {
            storeAccessPolicy.checkAccess(
                action = AccessAction.READ,
                resource = store,
            )
        }
    }

    @Test
    fun whenUserIsPartnerManagerAllowsAccessForCreateOrUpdateAction() {
        val managerId = "manager-1"
        val store = createStore()
        val partner = createPartner(managerId = managerId)

        givenCurrentUser(
            userId = managerId,
            roles = setOf(ApplicationRole.MANAGER),
        )

        `when`(partnerDBPort.findById(partnerId)).thenReturn(partner)

        assertDoesNotThrow {
            storeAccessPolicy.checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = store,
            )
        }
    }

    @Test
    fun whenUserIsPartnerStaffAllowsAccessForReadAction() {
        val store = createStore()

        givenCurrentUser(
            userId = "staff-1",
            roles = setOf(ApplicationRole.STAFF),
        )

        assertThrows<StoreAccessDeniedException> {
            storeAccessPolicy.checkAccess(
                action = AccessAction.CREATE_OR_UPDATE,
                resource = store,
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

    private fun createStore(
        id: StoreId = storeId,
        partnerId: PartnerId = this.partnerId,
    ): Store {
        val createdAt = Instant.parse("2026-01-01T10:00:00Z")

        return Store(
            id = id,
            partnerId = partnerId,
            name = "Test store",
            status = StoreStatus.entries.first(),
            workingHours = emptyList(),
            address = mock(Address::class.java),
            createdAt = createdAt,
            updatedAt = createdAt,
            version = 0,
        )
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
