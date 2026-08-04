package com.example.foodrescue.partnerservice.application.exception

import com.example.foodrescue.partnerservice.domain.entity.PartnerId

class PartnerNotFoundException(partnerId: PartnerId) :
    RuntimeException("Partner with id $partnerId was not found")
