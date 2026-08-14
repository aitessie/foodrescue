package com.example.foodrescue.partnerservice.application.exceptions

import com.example.foodrescue.partnerservice.domain.entities.PartnerId

class PartnerNotFoundException(partnerId: PartnerId) :
    RuntimeException("Partner with id $partnerId was not found")
