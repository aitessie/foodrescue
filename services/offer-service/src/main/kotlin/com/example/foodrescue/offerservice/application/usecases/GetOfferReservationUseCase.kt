package com.example.foodrescue.offerservice.application.usecases

import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException
import com.example.foodrescue.offerservice.application.exceptions.OfferReservationNotFoundException
import com.example.foodrescue.offerservice.application.ports.CurrentUserPort
import com.example.foodrescue.offerservice.application.ports.OfferReservationDBPort
import com.example.foodrescue.offerservice.domain.entities.OfferReservation
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import com.example.foodrescue.offerservice.domain.`enum`.ApplicationRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetOfferReservationUseCase(
    private val reservationDBPort: OfferReservationDBPort,
    private val currentUserPort: CurrentUserPort,
) {
    @Transactional(readOnly = true)
    fun execute(reservationId: ReservationId): OfferReservation {
        validateRole()

        val reservation =
            reservationDBPort.findById(reservationId)
                ?: throw OfferReservationNotFoundException(reservationId)

        if (currentUserPort.hasRole(ApplicationRole.ADMIN)) {
            return reservation
        }

        if (reservation.customerId != currentUserPort.getUserId()) {
            throw OfferReservationNotFoundException(reservationId)
        }

        return reservation
    }

    private fun validateRole() {
        val hasAllowedRole =
            currentUserPort.hasRole(ApplicationRole.CUSTOMER) ||
                currentUserPort.hasRole(ApplicationRole.ADMIN)

        if (!hasAllowedRole) {
            throw AccessDeniedException("Current user cannot read OfferReservation")
        }
    }
}
