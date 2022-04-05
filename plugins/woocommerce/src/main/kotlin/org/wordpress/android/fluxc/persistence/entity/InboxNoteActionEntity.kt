package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "InboxNoteActions",
    foreignKeys = [ForeignKey(
        entity = InboxNoteEntity::class,
        parentColumns = ["localId"],
        childColumns = ["inboxNoteLocalId"],
        onDelete = ForeignKey.CASCADE
    )],
    primaryKeys = ["id", "inboxNoteLocalId"]
)
data class InboxNoteActionEntity(
    val id: Long,
    val inboxNoteLocalId: Long,
    val siteId: Long,
    val name: String,
    val label: String,
    val url: String,
    val query: String? = null,
    val status: String? = null,
    val primary: Boolean = false,
    val actionedText: String? = null
)
