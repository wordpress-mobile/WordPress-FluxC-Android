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
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupWithAddons
import org.wordpress.android.fluxc.persistence.mappers.toAddonEntity
import org.wordpress.android.fluxc.persistence.mappers.toAddonGroupEntity
import org.wordpress.android.fluxc.persistence.mappers.toAddonOptionEntity

@Dao
internal abstract class AddonsDao {
    @Transaction
    @Query("SELECT * FROM GlobalAddonGroupEntity WHERE remoteSiteId = :remoteSiteId")
    abstract fun getGlobalAddonsForSite(remoteSiteId: Long): Flow<List<GlobalAddonGroupWithAddons>>

    @Insert
    abstract suspend fun insertGroup(globalAddonGroupEntity: GlobalAddonGroupEntity): Long

    @Insert
    abstract suspend fun insertAddons(addonEntities: AddonEntity): Long

    @Insert
    abstract suspend fun insertAddonOptions(vararg addonOptions: AddonOptionEntity)

    @Query("DELETE FROM GlobalAddonGroupEntity WHERE remoteSiteId = :remoteSiteId")
    abstract suspend fun deleteGlobalAddonsForSite(remoteSiteId: Long)

    @Transaction
    open suspend fun cacheGroups(
        globalAddonGroups: List<AddOnGroupDto>,
        remoteSiteId: Long
    ) {
        deleteGlobalAddonsForSite(remoteSiteId)

        globalAddonGroups.forEach { group ->
            val globalAddonGroupEntityId = insertGroup(group.toAddonGroupEntity(remoteSiteId))
            insertAddonEntity(group.addons, globalAddonGroupEntityId)
        }
    }

    private suspend fun insertAddonEntity(
        addons: List<WCProductAddonModel>,
        groupId: Long
    ) {
        addons.forEach { addon ->
            val addonEntity = addon.toAddonEntity(groupId)
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
