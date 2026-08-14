package com.example.foodrescue.partnerservice.application.exceptions

class EntityVersionConflictException(
    val entityType: String,
    val entityId: String,
    val expectedVersion: Long,
    val actualVersion: Long,
) :
    RuntimeException(
        "$entityType $entityId version conflict: " +
            "expected=$expectedVersion, actual=$actualVersion"
    )
