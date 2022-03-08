package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.OrderNoteEntity

@Dao
interface OrderNotesDao {
    @Insert(onConflict = REPLACE)
    suspend fun insertNotes(vararg notes: OrderNoteEntity)

    @Query("SELECT * FROM OrderNotes WHERE siteId = :siteId AND orderId = :orderId")
    suspend fun queryNotesOfOrder(siteId: RemoteId, orderId: RemoteId): List<OrderNoteEntity>

    @Query("DELETE FROM OrderNotes WHERE siteId = :siteId")
    fun deleteOrderNotesForSite(siteId: RemoteId)
}
