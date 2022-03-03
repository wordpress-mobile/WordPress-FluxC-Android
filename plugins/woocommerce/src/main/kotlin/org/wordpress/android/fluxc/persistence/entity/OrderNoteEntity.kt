package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(tableName = "OrderNoteEntity", primaryKeys = ["localSiteId", "noteId"])
data class OrderNoteEntity(
    val localSiteId: LocalId,
    val noteId: RemoteId,
    val orderId: RemoteId,
    @ColumnInfo(defaultValue = "")
    val dateCreated: String = "", // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @ColumnInfo(defaultValue = "")
    val note: String = "",
    @ColumnInfo(defaultValue = "")
    val author: String = "",
    @ColumnInfo(defaultValue = "0")
    val isSystemNote: Boolean = false, // True if the note is 'system-created', else created by a site user
    @ColumnInfo(defaultValue = "0")
    val isCustomerNote: Boolean = false // False if private, else customer-facing. Default is false
)
