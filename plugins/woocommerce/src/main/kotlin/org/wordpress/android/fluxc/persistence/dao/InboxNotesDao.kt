package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions

@Dao
abstract class InboxNotesDao {
    @Transaction
    @Query("SELECT * FROM InboxNotes WHERE siteId = :siteId ORDER BY dateCreated DESC, remoteId DESC")
    abstract fun observeInboxNotes(siteId: Long): Flow<List<InboxNoteWithActions>>

    @Transaction
    @Query("SELECT * FROM InboxNotes WHERE siteId = :siteId")
    abstract fun getInboxNotesForSite(siteId: Long): List<InboxNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInboxNote(entity: InboxNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInboxNoteAction(entity: InboxNoteActionEntity)

    @Transaction
    @Query("DELETE FROM InboxNotes WHERE siteId = :siteId")
    abstract fun deleteInboxNotesForSite(siteId: Long)

    @Query("DELETE FROM InboxNotes WHERE remoteId = :remoteNoteId AND siteId = :siteId")
    abstract suspend fun deleteInboxNote(remoteNoteId: Long, siteId: Long)

    @Transaction
    open suspend fun deleteAllAndInsertInboxNotes(
        siteId: Long,
        vararg notes: InboxNoteWithActions
    ) {
        deleteInboxNotesForSite(siteId)
        notes.forEach { noteWithActions ->
            val localNoteId = insertOrUpdateInboxNote(noteWithActions.inboxNote)
            noteWithActions.noteActions.forEach {
                insertOrUpdateInboxNoteAction(it.copy(inboxNoteLocalId = localNoteId))
            }
        }
    }

    @Transaction
    open suspend fun updateNote(siteId: Long, noteWithActions: InboxNoteWithActions) {
        deleteInboxNote(siteId, noteWithActions.inboxNote.remoteId)
        val localNoteId = insertOrUpdateInboxNote(noteWithActions.inboxNote)
        noteWithActions.noteActions.forEach {
            insertOrUpdateInboxNoteAction(it.copy(inboxNoteLocalId = localNoteId))
        }
    }
}
