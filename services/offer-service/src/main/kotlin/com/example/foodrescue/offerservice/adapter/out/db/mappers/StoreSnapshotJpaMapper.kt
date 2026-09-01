package com.example.foodrescue.offerservice.adapter.`out`.db.mappers

import com.example.foodrescue.offerservice.adapter.`out`.db.entities.StoreSnapshotJpaEntity
import com.example.foodrescue.offerservice.domain.entities.PartnerId
import com.example.foodrescue.offerservice.domain.entities.StoreId
import com.example.foodrescue.offerservice.domain.entities.StoreSnapshot
import java.time.ZoneId
import org.springframework.stereotype.Component

@Component
class StoreSnapshotJpaMapper {
    fun toDomain(entity: StoreSnapshotJpaEntity): StoreSnapshot =
        StoreSnapshot(
            storeId = StoreId(entity.storeId),
            partnerId = PartnerId(entity.partnerId),
            partnerStatus = entity.partnerStatus,
            storeStatus = entity.storeStatus,
            name = entity.name,
            address = entity.address,
            timeZone = ZoneId.of(entity.timeZone),
            storeVersion = entity.storeVersion,
            partnerVersion = entity.partnerVersion,
        )

    fun toJpaEntity(snapshot: StoreSnapshot): StoreSnapshotJpaEntity =
        StoreSnapshotJpaEntity(
            storeId = snapshot.storeId.value,
            partnerId = snapshot.partnerId.value,
            partnerStatus = snapshot.partnerStatus,
            storeStatus = snapshot.storeStatus,
            name = snapshot.name,
            address = snapshot.address,
            timeZone = snapshot.timeZone.id,
            storeVersion = snapshot.storeVersion,
            partnerVersion = snapshot.partnerVersion,
        )
}
