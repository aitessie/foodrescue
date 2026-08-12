package com.example.foodrescue.offerservice.application.exception

import com.example.foodrescue.offerservice.domain.entity.PartnerId
import com.example.foodrescue.offerservice.domain.entity.StoreId

class PartnerStoreNotFoundException(
    partnerId: PartnerId,
    storeId: StoreId,
) : NotFoundException("Partner/store hierarchy '$partnerId/$storeId' was not found")
