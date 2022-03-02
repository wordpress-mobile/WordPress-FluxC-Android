package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderNoteModel

@Dao
interface OrderNotesDao {
    @Insert(onConflict = REPLACE)
    suspend fun insertNotes(vararg notes: WCOrderNoteModel)

    @Query("SELECT * FROM OrderNoteEntity WHERE localSiteId = :localSiteId AND orderId = :orderId")
    suspend fun queryNotesOfOrder(localSiteId: LocalId, orderId: RemoteId): List<WCOrderNoteModel>

    @Query("DELETE FROM OrderNoteEntity WHERE localSiteId = :localSiteId")
    fun deleteOrderNotesForSite(localSiteId: LocalId)
}
