package org.wordpress.android.fluxc.model.product.attributes.terms

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey

data class WCAttributeTermModel(
    @PrimaryKey @Column private var id: Int = 0,
    @Column var localSiteId: Int = 0,
    @Column var name: String = "",
    @Column var slug: String = "",
    @Column var description: String = "",
    @Column var count: Int = 0,
    @Column var menuOrder: Int = 0
) : Identifiable {
    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = id
}
