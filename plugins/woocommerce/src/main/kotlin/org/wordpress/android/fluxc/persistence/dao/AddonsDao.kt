package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.dto.AddOnGroupDto
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.entity.AddonWithOptions
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupWithAddons
import org.wordpress.android.fluxc.persistence.mappers.ProductBasedIdentification
import org.wordpress.android.fluxc.persistence.mappers.toAddonEntity
import org.wordpress.android.fluxc.persistence.mappers.toAddonGroupEntity
import org.wordpress.android.fluxc.persistence.mappers.toAddonOptionEntity

@Dao
abstract class AddonsDao {
    @Transaction
    @Query("SELECT * FROM GlobalAddonGroupEntity WHERE siteRemoteId = :siteRemoteId")
    abstract fun observeGlobalAddonsForSite(siteRemoteId: Long): Flow<List<GlobalAddonGroupWithAddons>>

    @Transaction
    @Query("SELECT * FROM AddonEntity WHERE siteRemoteId = :siteRemoteId AND productRemoteId = :productRemoteId")
    abstract fun observeSingleProductAddons(siteRemoteId: Long, productRemoteId: Long): Flow<List<AddonWithOptions>>

    @Insert
    abstract suspend fun insertGroup(globalAddonGroupEntity: GlobalAddonGroupEntity): Long

    @Insert
    abstract suspend fun insertAddons(addonEntities: AddonEntity): Long

    @Insert
    abstract suspend fun insertAddonOptions(vararg addonOptions: AddonOptionEntity)

    @Query("DELETE FROM GlobalAddonGroupEntity WHERE siteRemoteId = :siteRemoteId")
    abstract suspend fun deleteGlobalAddonsForSite(siteRemoteId: Long)

    @Query("DELETE FROM AddonEntity WHERE productRemoteId = :productRemoteId AND siteRemoteId = :siteRemoteId")
    abstract suspend fun deleteAddonsForSpecifiedProduct(productRemoteId: Long, siteRemoteId: Long)

    @Transaction
    open suspend fun cacheGroups(
        globalAddonGroups: List<AddOnGroupDto>,
        siteRemoteId: Long
    ) {
        deleteGlobalAddonsForSite(siteRemoteId)

        globalAddonGroups.forEach { group ->
            val globalAddonGroupEntityId = insertGroup(group.toAddonGroupEntity(siteRemoteId))
            insertAddonEntity(group.addons, globalAddonGroupEntityId)
        }
    }

    @Transaction
    open suspend fun cacheProductAddons(
        productRemoteId: Long,
        siteRemoteId: Long,
        addons: List<WCProductAddonModel>
    ) {
        deleteAddonsForSpecifiedProduct(productRemoteId = productRemoteId, siteRemoteId = siteRemoteId)

        addons.forEach { addon ->
            val addonEntity = addon.toAddonEntity(
                    productBasedIdentification = ProductBasedIdentification(
                            siteRemoteId = siteRemoteId,
                            productRemoteId = productRemoteId
                    )
            )
            val addonEntityId = insertAddons(addonEntity)
            insertAddonOptionEntity(addon.options, addonEntityId)
        }
    }

    private suspend fun insertAddonEntity(
        addons: List<WCProductAddonModel>,
        groupId: Long
    ) {
        addons.forEach { addon ->
            val addonEntity = addon.toAddonEntity(globalGroupLocalId = groupId)
            val addonEntityId = insertAddons(addonEntity)
            insertAddonOptionEntity(addon.options, addonEntityId)
        }
    }

    private suspend fun insertAddonOptionEntity(
        options: List<WCProductAddonModel.ProductAddonOption>?,
        addonEntityId: Long
    ) {
        options?.forEach { addonOption ->
            val addonOptionEntity = addonOption.toAddonOptionEntity(addonLocalId = addonEntityId)
            insertAddonOptions(addonOptionEntity)
        }
    }
}
