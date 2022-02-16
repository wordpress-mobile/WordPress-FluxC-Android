package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.WCProductCategoryModel

@Entity(
    tableName = "ProductCategories",
    primaryKeys = ["id", "siteId"]
)
data class ProductCategoryEntity(
    val id: Long,
    val siteId: Long,
    val parentId: Long? = null,
    val name: String? = null,
    val slug: String? = null
)

fun WCProductCategoryModel.toDataModel(siteId: Long) =
    ProductCategoryEntity(
        remoteCategoryId,
        siteId,
        parent,
        name,
        slug
    )
