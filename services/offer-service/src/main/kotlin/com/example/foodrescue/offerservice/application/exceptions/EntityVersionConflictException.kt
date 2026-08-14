package com.example.foodrescue.offerservice.application.exceptions

class EntityVersionConflictException(
    aggregateType: String,
    aggregateId: String,
    suppliedVersion: Long,
    currentVersion: Long,
) :
    RuntimeException(
        "Version conflict for $aggregateType '$aggregateId': " +
            "supplied version $suppliedVersion does not match current version $currentVersion"
    )
