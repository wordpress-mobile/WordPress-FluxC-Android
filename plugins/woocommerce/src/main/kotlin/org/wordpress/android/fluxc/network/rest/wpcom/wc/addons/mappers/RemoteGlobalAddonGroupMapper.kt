package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers

import org.wordpress.android.fluxc.domain.GlobalAddonGroup
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.dto.AddOnGroupDto

object RemoteGlobalAddonGroupMapper {
    fun toDomain(dto: AddOnGroupDto): GlobalAddonGroup {
        return GlobalAddonGroup(
                name = dto.name,
                restrictedCategoriesIds = if (dto.categoryIds.isNullOrEmpty()) {
                    GlobalAddonGroup.CategoriesRestriction.AllProductsCategories
                } else {
                    GlobalAddonGroup.CategoriesRestriction.SpecifiedProductCategories(dto.categoryIds)
                },
                addons = dto.addons.map { dtoAddon -> RemoteAddonMapper.toDomain(dtoAddon) }
        )
    }
}
