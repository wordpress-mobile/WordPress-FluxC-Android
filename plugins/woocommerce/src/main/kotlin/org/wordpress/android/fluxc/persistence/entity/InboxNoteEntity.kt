package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Relation

data class InboxNoteWithActions(
    @Embedded val inboxNote: InboxNoteEntity,
    @Relation(
        parentColumn = "inboxNoteId",
        entityColumn = "inboxNoteId"
    )
    val noteActions: List<InboxNoteActionEntity>
)

@Entity(
    tableName = "InboxNotes",
    primaryKeys = ["inboxNoteId", "siteId"],
    indices = [Index("inboxNoteId", "siteId")]
)
data class InboxNoteEntity(
    val inboxNoteId: Long,
    val siteId: Long,
    val name: String,
    val isSnoozable: Boolean = false,
    val title: String,
    val content: String,
    val dateCreated: String,
    val status: InboxNoteStatus,
    val source: String? = null,
    val type: String? = null,
    val dateReminder: String? = null
){
    enum class InboxNoteStatus{
        Unactioned,
        Actioned,
        Snoozed
    }
}