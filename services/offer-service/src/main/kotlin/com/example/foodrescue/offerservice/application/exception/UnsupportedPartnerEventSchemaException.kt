package com.example.foodrescue.offerservice.application.exception

class UnsupportedPartnerEventSchemaException(schemaVersion: Int) :
    RuntimeException("Unsupported Partner event schema version: $schemaVersion")
