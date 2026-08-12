package com.example.foodrescue.offerservice.application.exception

import com.example.foodrescue.offerservice.domain.entity.ProductTemplateId

class ProductTemplateNotFoundException(productTemplateId: ProductTemplateId) :
    NotFoundException("Product template '$productTemplateId' was not found")
