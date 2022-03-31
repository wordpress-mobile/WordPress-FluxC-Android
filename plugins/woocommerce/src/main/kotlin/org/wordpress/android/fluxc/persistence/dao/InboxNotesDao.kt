package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions

@Dao
abstract class InboxNotesDao {
    @Transaction
    @Query("SELECT * FROM InboxNotes WHERE siteId = :siteId ORDER BY dateCreated DESC")
    abstract fun observeInboxNotes(siteId: Long): Flow<List<InboxNoteWithActions>>

    @Query("SELECT * FROM InboxNotes WHERE id = :inboxNoteId AND siteId = :siteId")
    abstract suspend fun getInboxNote(inboxNoteId: Long, siteId: Long): InboxNoteWithActions?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInboxNote(entity: InboxNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInboxNoteAction(entity: InboxNoteActionEntity)

    @Query("DELETE FROM InboxNotes WHERE siteId = :siteId")
    abstract fun deleteInboxNotesForSite(siteId: Long)

    @Transaction
    open suspend fun insertInboxNotesAndActions(siteId: Long, vararg notes: InboxNoteWithActions) {
        deleteInboxNotesForSite(siteId)
        notes.forEach { noteWithActions ->
            val localNoteId = insertOrUpdateInboxNote(noteWithActions.inboxNote)
            noteWithActions.noteActions.forEach {
                insertOrUpdateInboxNoteAction(it.copy(inboxNoteLocalId = localNoteId))
            }
        }
    }
}
