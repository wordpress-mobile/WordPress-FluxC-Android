package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

/**
 * Single Woo category - see https://woocommerce.github.io/woocommerce-rest-api-docs/?shell#product-category-properties
 * Note that categories have more properties than we support below
 */
@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCProductCategoryModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteCategoryId = 0L // The unique identifier for this category on the server
    @Column var name = ""
    @Column var slug = ""
    @Column var parent = 0L

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}
