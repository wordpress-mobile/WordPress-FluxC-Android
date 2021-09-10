package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.domain.GlobalAddonGroup
import org.wordpress.android.fluxc.domain.GlobalAddonGroup.CategoriesRestriction.AllProductsCategories
import org.wordpress.android.fluxc.domain.GlobalAddonGroup.CategoriesRestriction.SpecifiedProductCategories
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity

object ToDatabaseAddonGroupMapper {
    fun toEntityModel(domain: GlobalAddonGroup, siteRemoteId: Long): GlobalAddonGroupEntity {
        return GlobalAddonGroupEntity(
                name = domain.name,
                restrictedCategoriesIds = when (domain.restrictedCategoriesIds) {
                    AllProductsCategories -> emptyList()
                    is SpecifiedProductCategories -> domain.restrictedCategoriesIds.productCategories
                },
                siteRemoteId = siteRemoteId
        )
    }
}
