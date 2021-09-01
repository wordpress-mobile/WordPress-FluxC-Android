package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.dto.AddOnGroupDto
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity

internal fun AddOnGroupDto.toAddonGroupEntity(siteRemoteId: Long): GlobalAddonGroupEntity {
    return GlobalAddonGroupEntity(
            remoteId = this.id,
            name = this.name,
            restrictedCategoriesIds = this.categoryIds.orEmpty(),
            siteRemoteId = siteRemoteId
    )
}
