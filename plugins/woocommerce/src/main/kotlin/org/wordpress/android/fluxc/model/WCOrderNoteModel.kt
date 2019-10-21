package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCOrderNoteModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var localOrderId = 0 // The local db unique identifier for the parent order object
    @Column var remoteNoteId = 0L // The unique identifier for this note on the server
    @Column var dateCreated = "" // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var note = ""
    @Column var author = ""
    @Column var isSystemNote = false // True if the note is 'system-created', else created by a site user
        @JvmName("setIsSystemNote")
        set
    @Column var isCustomerNote = false // False if private, else customer-facing. Default is false
        @JvmName("setIsCustomerNote")
        set

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}
