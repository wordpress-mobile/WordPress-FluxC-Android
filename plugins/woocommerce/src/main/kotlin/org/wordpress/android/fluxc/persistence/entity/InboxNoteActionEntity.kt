package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "InboxNoteActions",
    foreignKeys = [ForeignKey(
        entity = InboxNoteEntity::class,
        parentColumns = ["inboxNoteId"],
        childColumns = ["inboxNoteId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class InboxNoteActionEntity(
    @PrimaryKey val actionId: Long,
    val inboxNoteId: Long,
    val name: String,
    val label: String,
    val url: String,
    val query: String? = null,
    val status: String? = null,
    val primary: Boolean = false,
    val actionedText: String? = null
)