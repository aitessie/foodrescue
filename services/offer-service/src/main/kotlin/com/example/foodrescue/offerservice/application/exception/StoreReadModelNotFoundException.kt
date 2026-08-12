package com.example.foodrescue.offerservice.application.exception

import com.example.foodrescue.offerservice.domain.entity.StoreId

class StoreReadModelNotFoundException(storeId: StoreId) :
    NotFoundException("Store '$storeId' was not found")
