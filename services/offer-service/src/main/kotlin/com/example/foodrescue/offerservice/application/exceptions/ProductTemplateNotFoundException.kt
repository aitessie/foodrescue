package com.example.foodrescue.offerservice.application.exceptions

import com.example.foodrescue.offerservice.domain.entities.ProductTemplateId

class ProductTemplateNotFoundException(productTemplateId: ProductTemplateId) :
    NotFoundException("Product template '$productTemplateId' was not found")
