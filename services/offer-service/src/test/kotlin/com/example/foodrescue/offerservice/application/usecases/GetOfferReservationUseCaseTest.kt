package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.OfferReservationNotFoundException
import com.example.foodrescue.offerservice.application.ports.CurrentUserPort
import com.example.foodrescue.offerservice.application.ports.OfferReservationDBPort
import com.example.foodrescue.offerservice.domain.entities.OfferId
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import com.example.foodrescue.offerservice.domain.enum.ApplicationRole
import com.example.foodrescue.offerservice.domain.enum.ReservationStatus
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GetOfferReservationUseCaseTest {
    @Mock private lateinit var reservationDBPort: OfferReservationDBPort

    @Mock private lateinit var currentUserPort: CurrentUserPort

    @InjectMocks private lateinit var useCase: GetOfferReservationUseCase

    @Test
    fun whenCustomerRequestsOwnReservation_returnsReservation() {
        // Arrange
        val reservation = createOfferReservation(customerId = CURRENT_USER_ID)

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)

        // Act
        val result = useCase.execute(reservationId = reservation.id)

        // Assert
        assertThat(result).isSameAs(reservation)

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(reservationDBPort).findById(reservation.id)
        verify(currentUserPort).getUserId()
        verifyNoMoreInteractions(
            reservationDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenAdminRequestsForeignReservation_returnsReservation() {
        // Arrange
        val reservation = createOfferReservation(customerId = OTHER_USER_ID)

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(true)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)

        // Act
        val result = useCase.execute(reservationId = reservation.id)

        // Assert
        assertThat(result).isSameAs(reservation)

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, times(2)).hasRole(ApplicationRole.ADMIN)
        verify(reservationDBPort).findById(reservation.id)
        verify(currentUserPort, never()).getUserId()
        verifyNoMoreInteractions(
            reservationDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenCustomerRequestsForeignReservation_throwsOfferReservationNotFoundException() {
        // Arrange
        val reservation = createOfferReservation(customerId = OTHER_USER_ID)

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(reservationDBPort.findById(reservation.id)).thenReturn(reservation)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)
        `when`(currentUserPort.getUserId()).thenReturn(CURRENT_USER_ID)

        // Act
        val exception =
            assertThrows<OfferReservationNotFoundException> {
                useCase.execute(reservationId = reservation.id)
            }

        // Assert
        assertThat(exception.message).contains(reservation.id.value.toString())

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verify(reservationDBPort).findById(reservation.id)
        verify(currentUserPort).getUserId()
        verifyNoMoreInteractions(
            reservationDBPort,
            currentUserPort,
        )
    }

    @Test
    fun whenCustomerRequestsMissingReservation_throwsOfferReservationNotFoundException() {
        // Arrange
        val reservationId = ReservationId(UUID.randomUUID())

        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(true)
        `when`(reservationDBPort.findById(reservationId)).thenReturn(null)

        // Act
        val exception =
            assertThrows<OfferReservationNotFoundException> {
                useCase.execute(reservationId = reservationId)
            }

        // Assert
        assertThat(exception.message).contains(reservationId.value.toString())

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort, never()).hasRole(ApplicationRole.ADMIN)
        verify(reservationDBPort).findById(reservationId)
        verify(currentUserPort, never()).getUserId()
        verifyNoMoreInteractions(
            reservationDBPort,
            currentUserPort,
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = ApplicationRole::class,
        names =
            [
                "STAFF",
                "MANAGER",
            ],
    )
    fun whenManagementRoleRequestsReservation_throwsAccessDeniedException(role: ApplicationRole) {
        // Arrange
        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER))
            .thenReturn(role == ApplicationRole.CUSTOMER)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN))
            .thenReturn(role == ApplicationRole.ADMIN)

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                useCase.execute(reservationId = ReservationId(UUID.randomUUID()))
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user cannot read OfferReservation")

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verifyNoInteractions(reservationDBPort)
        verifyNoMoreInteractions(currentUserPort)
    }

    @Test
    fun whenUserHasNoApplicationRole_throwsAccessDeniedException() {
        // Arrange
        `when`(currentUserPort.hasRole(ApplicationRole.CUSTOMER)).thenReturn(false)
        `when`(currentUserPort.hasRole(ApplicationRole.ADMIN)).thenReturn(false)

        // Act
        val exception =
            assertThrows<AccessDeniedException> {
                useCase.execute(reservationId = ReservationId(UUID.randomUUID()))
            }

        // Assert
        assertThat(exception.message).isEqualTo("Current user cannot read OfferReservation")

        verify(currentUserPort).hasRole(ApplicationRole.CUSTOMER)
        verify(currentUserPort).hasRole(ApplicationRole.ADMIN)
        verifyNoInteractions(reservationDBPort)
        verifyNoMoreInteractions(currentUserPort)
    }

    private fun createOfferReservation(customerId: String = CURRENT_USER_ID): OfferReservation =
        OfferReservation(
            id = ReservationId(UUID.randomUUID()),
            offerId = OfferId(UUID.randomUUID()),
            customerId = customerId,
            quantity = 2,
            status = ReservationStatus.RESERVED,
            createdAt = Instant.parse("2026-08-20T10:00:00Z"),
            updatedAt = Instant.parse("2026-08-20T10:00:00Z"),
            version = 0,
        )

    companion object {
        private const val CURRENT_USER_ID = "33333333-3333-3333-3333-333333333333"

        private const val OTHER_USER_ID = "88888888-8888-8888-8888-888888888888"
    }
}
