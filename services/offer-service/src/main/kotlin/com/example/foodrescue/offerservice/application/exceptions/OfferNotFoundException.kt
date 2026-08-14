package com.example.foodrescue.offerservice.application.exceptions

import com.example.foodrescue.offerservice.domain.entities.OfferId

class OfferNotFoundException(offerId: OfferId) : NotFoundException("Offer '$offerId' was not found")
