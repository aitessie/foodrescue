package com.example.foodrescue.partnerservice.application.exception

class EntityVersionConflictException(
    entityType: String,
    entityId: String,
    expectedVersion: Long,
    actualVersion: Long,
) : RuntimeException(
    "$entityType $entityId version conflict: " +
        "expected=$expectedVersion, actual=$actualVersion"
)
