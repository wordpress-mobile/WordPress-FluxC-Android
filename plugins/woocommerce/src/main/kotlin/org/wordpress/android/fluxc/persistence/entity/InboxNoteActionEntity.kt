package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "InboxNoteActions",
    foreignKeys = [ForeignKey(
        entity = InboxNoteEntity::class,
        parentColumns = ["id", "siteId"],
        childColumns = ["inboxNoteId", "siteId"],
        onDelete = ForeignKey.CASCADE
    )],
    primaryKeys = ["id", "inboxNoteId", "siteId"],
)
data class InboxNoteActionEntity(
    val id: Long,
    val inboxNoteId: Long,
    val siteId: Long,
    val name: String,
    val label: String,
    val url: String,
    val query: String? = null,
    val status: String? = null,
    val primary: Boolean = false,
    val actionedText: String? = null
)
