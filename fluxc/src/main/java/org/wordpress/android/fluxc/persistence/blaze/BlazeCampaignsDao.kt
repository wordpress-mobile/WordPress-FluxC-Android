package org.wordpress.android.fluxc.persistence.blaze

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.persistence.coverters.BlazeCampaignsDateConverter
import java.util.Date

@Dao
abstract class BlazeCampaignsDao {
    @Transaction
    open suspend fun getCampaignsAndPaginationForSite(siteId: Long): BlazeCampaignsModel {
        val campaigns = getCampaigns(siteId)
        val pagination = getCampaignsPagination(siteId)
        return BlazeCampaignsModel(
            campaigns = campaigns?.map { it.toDomainModel() } ?: emptyList(),
            page = pagination?.page ?: 1,
            totalItems = pagination?.totalItems ?: 0,
            totalPages = pagination?.totalPages ?: 0
        )
    }

    @Query("SELECT * from BlazeCampaigns WHERE `siteId` = :siteId ORDER BY startDate DESC")
    abstract fun getCampaigns(siteId: Long): List<BlazeCampaignEntity>?

    @Query("SELECT * from BlazeCampaignsPagination WHERE `siteId` = :siteId")
    abstract fun getCampaignsPagination(siteId: Long): BlazeCampaignsPaginationEntity?

    @Transaction
    @Query("SELECT * from BlazeCampaigns WHERE `siteId` = :siteId ORDER BY startDate DESC")
    fun getMostRecentCampaignForSite(siteId: Long): BlazeCampaignModel? {
        return getCampaigns(siteId)?.firstOrNull()?.toDomainModel()
    }

    @Transaction
    open suspend fun insertCampaignsAndPageInfoForSite(
        siteId: Long,
        domainModel: BlazeCampaignsModel
    ) {
        if (domainModel.page == 1) {
            clearBlazeCampaigns(siteId)
            clearBlazeCampaignsPagination(siteId)
        } // Always clear both tables when inserting first page
        insertCampaignsForSite(siteId, domainModel)
        insertCampaignsPaginationForSite(siteId, domainModel)
    }

    private suspend fun insertCampaignsForSite(siteId: Long, domainModel: BlazeCampaignsModel) {
        insert(domainModel.campaigns.map { BlazeCampaignEntity.fromDomainModel(siteId, it) })
    }

    private suspend fun insertCampaignsPaginationForSite(
        siteId: Long,
        domainModel: BlazeCampaignsModel
    ) {
        insert(BlazeCampaignsPaginationEntity.fromDomainModel(siteId, domainModel))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(campaigns: List<BlazeCampaignEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(campaigns: BlazeCampaignsPaginationEntity)

    @Transaction
    open fun clear(siteId: Long) {
        clearBlazeCampaigns(siteId)
        clearBlazeCampaignsPagination(siteId)
    }

    @Query("DELETE FROM BlazeCampaigns where siteId = :siteId")
    abstract fun clearBlazeCampaigns(siteId: Long)

    @Query("DELETE FROM BlazeCampaignsPagination where siteId = :siteId")
    abstract fun clearBlazeCampaignsPagination(siteId: Long)

    @Entity(
        tableName = "BlazeCampaigns",
        indices = [Index(value = ["siteId"], unique = false)],
        primaryKeys = ["siteId", "campaignId"]
    )
    @TypeConverters(BlazeCampaignsDateConverter::class)
    data class BlazeCampaignEntity(
        val siteId: Long,
        val campaignId: Long,
        val title: String,
        val imageUrl: String,
        val startDate: Date,
        val endDate: Date?,
        val uiStatus: String,
        val budgetCents: Double,
        val impressions: Long,
        val clicks: Long,
    ) {
        fun toDomainModel() = BlazeCampaignModel(
            campaignId = campaignId,
            title = title,
            imageUrl = imageUrl,
            startDate = startDate,
            endDate = endDate,
            uiStatus = uiStatus,
            budgetCents = budgetCents,
            impressions = impressions,
            clicks = clicks
        )

        companion object {
            fun fromDomainModel(
                siteId: Long,
                campaign: BlazeCampaignModel
            ) = BlazeCampaignEntity(
                siteId = siteId,
                campaignId = campaign.campaignId,
                title = campaign.title,
                imageUrl = campaign.imageUrl,
                startDate = campaign.startDate,
                endDate = campaign.endDate,
                uiStatus = campaign.uiStatus,
                budgetCents = campaign.budgetCents,
                impressions = campaign.impressions,
                clicks = campaign.clicks
            )
        }
    }

    @Entity(
        tableName = "BlazeCampaignsPagination",
        primaryKeys = ["siteId"]
    )
    data class BlazeCampaignsPaginationEntity(
        val siteId: Long,
        val page: Int,
        val totalItems: Int,
        val totalPages: Int
    ) {
        companion object {
            fun fromDomainModel(
                siteId: Long,
                domainModel: BlazeCampaignsModel
            ) = BlazeCampaignsPaginationEntity(
                siteId = siteId,
                page = domainModel.page,
                totalItems = domainModel.totalItems,
                totalPages = domainModel.totalPages
            )
        }
    }
}