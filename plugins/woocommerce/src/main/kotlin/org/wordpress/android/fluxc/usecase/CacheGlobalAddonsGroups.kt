package org.wordpress.android.fluxc.usecase

import org.wordpress.android.fluxc.model.addons.WCProductAddonModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.dto.AddOnGroupDto
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.mappers.toAddonEntity
import org.wordpress.android.fluxc.persistence.mappers.toAddonGroupEntity
import org.wordpress.android.fluxc.persistence.mappers.toAddonOptionEntity
import javax.inject.Inject

internal class CacheGlobalAddonsGroups @Inject constructor(private val addonsDao: AddonsDao) {
    suspend fun invoke(
        globalAddonGroups: List<AddOnGroupDto>,
        remoteSiteId: Long
    ) {
        addonsDao.deleteGlobalAddonsForSite(remoteSiteId)

        globalAddonGroups.forEach { group ->
            val globalAddonGroupEntityId = addonsDao.insertGroup(group.toAddonGroupEntity(remoteSiteId))
            insertAddonEntity(group.addons, globalAddonGroupEntityId)
        }
    }

    private suspend fun insertAddonEntity(
        addons: List<WCProductAddonModel>,
        groupId: Long
    ) {
        addons.forEach { addon ->
            val addonEntity = addon.toAddonEntity(groupId)
            val addonEntityId = addonsDao.insertAddons(addonEntity)
            insertAddonOptionEntity(addon.options, addonEntityId)
        }
    }

    private suspend fun insertAddonOptionEntity(
        options: List<WCProductAddonModel.ProductAddonOption>?,
        addonEntityId: Long
    ) {
        options?.forEach { addonOption ->
            val addonOptionEntity = addonOption.toAddonOptionEntity(addonLocalId = addonEntityId)
            addonsDao.insertAddonOptions(addonOptionEntity)
        }
    }
}
