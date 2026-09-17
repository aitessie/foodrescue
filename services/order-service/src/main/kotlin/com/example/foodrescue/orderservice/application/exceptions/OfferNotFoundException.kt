package com.example.foodrescue.orderservice.application.exceptions

import com.example.foodrescue.orderservice.domain.entities.OfferId

class OfferNotFoundException(offerId: OfferId) :
    RuntimeException("Offer not found: ${offerId.value}")
