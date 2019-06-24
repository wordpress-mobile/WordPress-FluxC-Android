package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
@RawConstraints(
        "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE",
        "UNIQUE (REMOTE_ORDER_ID, LOCAL_SITE_ID) ON CONFLICT REPLACE"
)
data class WCOrderSummaryModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteOrderId = 0L // The unique identifier for this order on the server
    @Column var dateCreated = "" // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    var dateModified = "" // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}
