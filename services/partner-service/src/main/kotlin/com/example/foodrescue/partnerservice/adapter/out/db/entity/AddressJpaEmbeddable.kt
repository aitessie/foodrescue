package com.example.foodrescue.partnerservice.adapter.out.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class AddressJpaEmbeddable(
    @Column(name = "address_city", nullable = false) var city: String,
    @Column(name = "address_street", nullable = false) var street: String,
    @Column(name = "address_building", nullable = false) var building: String,
    @Column(name = "address_postal_code") var postalCode: String?,
)
