package com.example.foodrescue.offerservice.adapter.`in`

import com.example.foodrescue.offerservice.adapter.`in`.dtos.OfferReservationDto
import com.example.foodrescue.offerservice.adapter.`in`.mappers.OfferRestMapper
import com.example.foodrescue.offerservice.application.usecases.GetOfferReservationUseCase
import com.example.foodrescue.offerservice.application.usecases.ReleaseFoodBagReservationUseCase
import com.example.foodrescue.offerservice.domain.entities.ReservationId
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/reservations")
class OfferReservationController(
    private val getOfferReservationUseCase: GetOfferReservationUseCase,
    private val releaseFoodBagReservationUseCase: ReleaseFoodBagReservationUseCase,
    private val offerRestMapper: OfferRestMapper,
) {
    @GetMapping("/{reservationId}")
    fun getOfferReservation(@PathVariable reservationId: UUID): OfferReservationDto {
        val reservation = getOfferReservationUseCase.execute(ReservationId(reservationId))

        return offerRestMapper.toReservationDto(reservation)
    }

    @DeleteMapping("/{reservationId}")
    fun releaseFoodBagReservation(@PathVariable reservationId: UUID): ResponseEntity<Void> {
        releaseFoodBagReservationUseCase.execute(ReservationId(reservationId))

        return ResponseEntity.noContent().build()
    }
}
