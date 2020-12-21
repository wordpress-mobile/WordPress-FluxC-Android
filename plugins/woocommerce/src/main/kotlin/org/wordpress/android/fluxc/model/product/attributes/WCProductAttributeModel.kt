package org.wordpress.android.fluxc.model.product.attributes

import com.yarolegovich.wellsql.core.Identifiable

data class WCProductAttributeModel(
    private var id: Int = 0,
    var name: String = "",
    var slug: String = "",
    var type: String = "",
    var orderBy: String = "",
    var hasArchives: Boolean = false
) : Identifiable {
    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = id
}
