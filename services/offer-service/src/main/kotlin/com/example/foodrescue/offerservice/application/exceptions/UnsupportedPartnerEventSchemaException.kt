package com.example.foodrescue.offerservice.application.exceptions

class UnsupportedPartnerEventSchemaException(schemaVersion: Int) :
    RuntimeException("Unsupported Partner event schema version: $schemaVersion")
