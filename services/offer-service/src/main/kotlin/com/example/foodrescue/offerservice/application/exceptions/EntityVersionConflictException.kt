package com.example.foodrescue.offerservice.application.exceptions

import java.util.UUID

class EntityVersionConflictException(
    entityName: String,
    entityId: UUID,
    requestedVersion: Long,
    currentVersion: Long,
) :
    RuntimeException(
        "Version conflict for $entityName '$entityId': " +
            "supplied version $requestedVersion does not match current version $currentVersion"
    )
