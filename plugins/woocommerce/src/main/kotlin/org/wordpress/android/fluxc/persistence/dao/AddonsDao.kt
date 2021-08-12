package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupWithAddons

@Dao
internal interface AddonsDao {
    @Transaction
    @Query("SELECT * FROM GlobalAddonGroupEntity WHERE remoteSiteId = :remoteSiteId")
    fun getGlobalAddonsForSite(remoteSiteId: Long): Flow<List<GlobalAddonGroupWithAddons>>

    @Insert
    suspend fun insertGroup(globalAddonGroupEntity: GlobalAddonGroupEntity): Long

    @Insert
    suspend fun insertAddons(addonEntities: AddonEntity): Long

    @Insert
    suspend fun insertAddonOptions(vararg addonOptions: AddonOptionEntity)

    @Query("DELETE FROM GlobalAddonGroupEntity WHERE remoteSiteId = :remoteSiteId")
    suspend fun deleteGlobalAddonsForSite(remoteSiteId: Long)
}
