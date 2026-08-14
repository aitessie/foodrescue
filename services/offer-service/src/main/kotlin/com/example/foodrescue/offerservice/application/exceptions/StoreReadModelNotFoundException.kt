package com.example.foodrescue.offerservice.application.exceptions

import com.example.foodrescue.offerservice.domain.entities.StoreId

class StoreReadModelNotFoundException(storeId: StoreId) :
    NotFoundException("Store '$storeId' was not found")
