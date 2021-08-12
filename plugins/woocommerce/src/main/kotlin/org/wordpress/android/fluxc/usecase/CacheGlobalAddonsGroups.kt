package org.wordpress.android.fluxc.usecase

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
            group.addons.forEach { addon ->
                val addonEntity = addon.toAddonEntity(globalAddonGroupEntityId)
                val addonEntityId = addonsDao.insertAddons(addonEntity)
                addon.options?.forEach { addonOption ->
                    val addonOptionEntity = addonOption.toAddonOptionEntity(addonLocalId = addonEntityId)
                    addonsDao.insertAddonOptions(addonOptionEntity)
                }
            }
        }
    }
}
