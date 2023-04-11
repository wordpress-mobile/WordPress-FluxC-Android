package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(
    tableName = "BundledProduct",
    primaryKeys = ["productId", "localSiteId", "bundledItemId"]
)
data class BundledProductEntity(
    @ColumnInfo(name = "localSiteId")
    val localSiteId: LocalId,
    val bundledItemId: Long,
    val productId: RemoteId,
    val bundledProductId: RemoteId,
    val menuOrder: Int,
    val title: String,
    val stockStatus: String
)
