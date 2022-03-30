package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Relation

data class InboxNoteWithActions(
    @Embedded val inboxNote: InboxNoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "inboxNoteId"
    )
    val noteActions: List<InboxNoteActionEntity>
)

@Entity(
    tableName = "InboxNotes",
    primaryKeys = ["id", "siteId"],
    indices = [Index("id", "siteId")]
)
data class InboxNoteEntity(
    val id: Long,
    val siteId: Long,
    val name: String,
    val isSnoozable: Boolean = false,
    val title: String,
    val content: String,
    val dateCreated: String,
    val status: LocalInboxNoteStatus,
    val source: String? = null,
    val type: String? = null,
    val dateReminder: String? = null
){
    enum class LocalInboxNoteStatus{
        Unactioned,
        Actioned,
        Snoozed
    }
}
