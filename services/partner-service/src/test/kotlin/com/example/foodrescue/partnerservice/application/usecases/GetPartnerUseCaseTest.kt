package com.example.foodrescue.partnerservice.application.usecases

import com.example.foodrescue.partnerservice.application.access.PartnerAccessPolicy
import com.example.foodrescue.partnerservice.application.exceptions.PartnerAccessDeniedException
import com.example.foodrescue.partnerservice.application.exceptions.PartnerNotFoundException
import com.example.foodrescue.partnerservice.application.ports.PartnerDBPort
import com.example.foodrescue.partnerservice.domain.entities.Partner
import com.example.foodrescue.partnerservice.domain.entities.PartnerId
import com.example.foodrescue.partnerservice.domain.enum.AccessAction
import com.example.foodrescue.partnerservice.domain.enum.PartnerStatus
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class GetPartnerUseCaseTest {
    private val partnerDBPort = mock(PartnerDBPort::class.java)
    private val partnerAccessPolicy = mock(PartnerAccessPolicy::class.java)
    private val useCase =
        GetPartnerUseCase(
            partnerDBPort = partnerDBPort,
            partnerAccessPolicy = partnerAccessPolicy,
        )

    @Test
    fun whenPartnerExistsAndAccessAllowedReturnsPartner() {
        // arrange
        val partner = createPartner()

        `when`(partnerDBPort.findById(partner.id)).thenReturn(partner)

        // act
        val result = useCase.getPartner(partner.id)

        // assert
        assertThat(partner).isEqualTo(result)

        verify(partnerDBPort).findById(partner.id)
        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.READ,
                resource = partner,
            )
    }

    @Test
    fun whenAccessDeniedThrowsPartnerAccessDeniedException() {
        // arrange
        val partner = createPartner()

        `when`(partnerDBPort.findById(partner.id)).thenReturn(partner)

        // act
        doThrow(PartnerAccessDeniedException())
            .`when`(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.READ,
                resource = partner,
            )

        // assert
        assertThrows(PartnerAccessDeniedException::class.java) {
            useCase.getPartner(partner.id)
        }

        verify(partnerDBPort).findById(partner.id)
        verify(partnerAccessPolicy)
            .checkAccess(
                action = AccessAction.READ,
                resource = partner,
            )
    }

    @Test
    fun whenPartnerDoesNotExistDoesNotCheckAccess() {
        // arrange
        val partnerId = PartnerId(UUID.randomUUID())

        `when`(partnerDBPort.findById(partnerId)).thenThrow(PartnerNotFoundException(partnerId))

        // assert
        assertThrows(PartnerNotFoundException::class.java) {
            useCase.getPartner(partnerId)
        }

        verify(partnerDBPort).findById(partnerId)
        verifyNoInteractions(partnerAccessPolicy)
    }

    private fun createPartner(): Partner {
        val timestamp = Instant.parse("2026-08-05T10:00:00Z")

        return Partner(
            id = PartnerId(UUID.randomUUID()),
            managerId = UUID.randomUUID().toString(),
            name = "Test partner",
            status = PartnerStatus.entries.first(),
            createdAt = timestamp,
            updatedAt = timestamp,
            version = 0,
        )
    }
}
