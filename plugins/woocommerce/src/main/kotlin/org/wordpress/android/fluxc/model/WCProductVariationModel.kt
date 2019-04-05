package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

/**
 * Product variations - see http://woocommerce.github.io/woocommerce-rest-api-docs/#product-variations
 * As with WCProductModel, the backend returns more properties than are supported below
 */
@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCProductVariationModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteProductId = 0L
    @Column var remoteVariationId = 0L

    @Column var dateCreated = ""
    @Column var dateModified = ""

    @Column var description = ""
    @Column var permalink = ""
    @Column var sku = ""
    @Column var status = ""

    @Column var price = ""
    @Column var regularPrice = ""
    @Column var salePrice = ""

    @Column var onSale = false
    @Column var purchasable = false
    @Column var downloadable = false
    @Column(name = "IS_VIRTUAL") var virtual = false

    @Column var manageStock = false
    @Column var stockQuantity = 0
    @Column var stockStatus = ""

    @Column var image = ""

    @Column var weight = ""
    @Column var length = ""
    @Column var width = ""
    @Column var height = ""

    @Column var attributes = ""

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}
