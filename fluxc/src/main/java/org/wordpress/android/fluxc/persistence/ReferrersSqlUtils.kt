package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.ReferrerGroupTable
import com.wellsql.generated.ReferrerTable
import com.wellsql.generated.ReferrersModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel.Group
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel.Referrer
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferrersSqlUtils
@Inject constructor(private val statsUtils: StatsUtils) {
    fun insert(
        site: SiteModel,
        statsGranularity: StatsGranularity,
        item: ReferrersModel,
        date: Date? = null
    ) {
        val formattedDate = statsUtils.getFormattedDate(date)
        val statsType = statsGranularity.toStatsType()
        deleteAll(site, statsType, formattedDate)
        val insertedModel = ReferrersModelBuilder(
                localSiteId = site.id,
                statsType = statsType.name,
                date = formattedDate,
                otherViews = item.otherViews,
                totalViews = item.totalViews,
                hasMore = item.hasMore
        )
        WellSql.insert(insertedModel).execute()
        if (insertedModel.id != -1) {
            val insertedGroups = item.groups.map { group ->
                ReferrerGroupBuilder(
                        modelId = insertedModel.id,
                        groupId = group.groupId,
                        name = group.name,
                        icon = group.icon,
                        url = group.url,
                        markedAsSpam = group.markedAsSpam ?: false,
                        total = group.total
                ) to group.referrers
            }.toMap()
            WellSql.insert(insertedGroups.keys.toList()).execute()
            val referrerBuilders = mutableListOf<ReferrerBuilder>()
            insertedGroups.forEach { (dbModel, referrers) ->
                referrerBuilders.addAll(referrers.map { referrer ->
                    ReferrerBuilder(
                            groupId = dbModel.id,
                            name = referrer.name,
                            url = referrer.url,
                            icon = referrer.icon,
                            views = referrer.views,
                            markedAsSpam = referrer.markedAsSpam ?: false
                    )
                })
            }
            WellSql.insert(referrerBuilders).execute()
        }
    }

    fun select(
        site: SiteModel,
        statsGranularity: StatsGranularity,
        date: Date? = null
    ): ReferrersModel? {
        val statsType = statsGranularity.toStatsType()
        val formattedDate = statsUtils.getFormattedDate(date)
        val model = createSelectStatement(site, statsType, formattedDate).asModel.firstOrNull()
        return model?.let {
            val groups = selectGroups(it.id)
            ReferrersModel(
                    otherViews = it.otherViews,
                    totalViews = it.totalViews,
                    groups = groups,
                    hasMore = it.hasMore
            )
        }
    }

    private fun selectGroups(modelId: Int): List<Group> {
        val groups = WellSql.select(ReferrerGroupBuilder::class.java)
                .where()
                .equals(ReferrerGroupTable.MODEL_ID, modelId).endWhere().asModel
        return groups.map { dbModel ->
            val referrers = selectReferrers(dbModel.id)
            Group(dbModel.groupId, dbModel.name, dbModel.icon, dbModel.url, dbModel.total, referrers)
        }
    }

    private fun selectGroups(modelIds: List<Int>): List<ReferrerGroupBuilder> {
        if (modelIds.isEmpty()) {
            return listOf()
        }
        return WellSql.select(ReferrerGroupBuilder::class.java)
                .where()
                .isIn(ReferrerGroupTable.MODEL_ID, modelIds).endWhere().asModel
    }

    private fun selectReferrers(groupId: Int): List<Referrer> {
        val groups = WellSql.select(ReferrerBuilder::class.java)
                .where()
                .equals(ReferrerTable.GROUP_ID, groupId).endWhere().asModel
        return groups.map { dbModel ->
            Referrer(
                    dbModel.name,
                    dbModel.views,
                    dbModel.icon,
                    dbModel.url,
                    dbModel.markedAsSpam
            )
        }
    }

    fun deleteAllStats(): Int {
        val deleteModel = WellSql.delete(ReferrersModelBuilder::class.java).execute()
        val deleteGroups = WellSql.delete(ReferrerGroupBuilder::class.java).execute()
        val deleteReferrers = WellSql.delete(ReferrerBuilder::class.java).execute()
        return deleteModel + deleteGroups + deleteReferrers
    }

    fun deleteSiteStats(site: SiteModel) = deleteAll(site)

    private fun deleteAll(
        site: SiteModel,
        statsType: StatsType? = null,
        formattedDate: String? = null
    ) {
        val modelIds = createSelectStatement(site, statsType, formattedDate).asModel.map { it.id }
        val groupIds = selectGroups(modelIds).map { it.id }
        deleteReferrers(groupIds)
        deleteGroups(modelIds)
        if (modelIds.isNotEmpty()) {
            WellSql.delete(ReferrersModelBuilder::class.java)
                    .where()
                    .isIn(ReferrersModelTable.ID, modelIds).endWhere().execute()
        }
    }

    private fun deleteGroups(modelIds: List<Int>): Int {
        if (modelIds.isEmpty()) {
            return 0
        }
        return WellSql.delete(ReferrerGroupBuilder::class.java).where()
                .isIn(ReferrerGroupTable.MODEL_ID, modelIds)
                .endWhere().execute()
    }

    private fun deleteReferrers(groupIds: List<Int>): Int {
        if (groupIds.isEmpty()) {
            return 0
        }
        return WellSql.delete(ReferrerBuilder::class.java).where()
                .isIn(ReferrerTable.GROUP_ID, groupIds)
                .endWhere().execute()
    }

    private fun createSelectStatement(
        site: SiteModel,
        statsType: StatsType? = null,
        date: String? = null
    ): SelectQuery<ReferrersModelBuilder> {
        var select = WellSql.select(ReferrersModelBuilder::class.java)
                .where()
                .equals(ReferrersModelTable.LOCAL_SITE_ID, site.id)

        if (statsType != null) {
            select = select.equals(ReferrersModelTable.STATS_TYPE, statsType.name)
        }
        if (date != null) {
            select = select.equals(ReferrersModelTable.DATE, date)
        }
        return select.endWhere()
    }

    @Table(name = "ReferrersModel")
    data class ReferrersModelBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var statsType: String,
        @Column var date: String?,
        @Column var otherViews: Int,
        @Column var totalViews: Int,
        @Column var hasMore: Boolean
    ) : Identifiable {
        constructor() : this(-1, -1, "", null, 0, 0, false)

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }

    @Table(name = "ReferrerGroup")
    data class ReferrerGroupBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var modelId: Int,
        @Column var groupId: String?,
        @Column var name: String?,
        @Column var icon: String?,
        @Column var url: String?,
        @Column var total: Int?,
        @Column var markedAsSpam: Boolean
    ) : Identifiable {
        constructor() : this(-1, -1, null, null, null, null, null, false)

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }

    @Table(name = "Referrer")
    data class ReferrerBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var groupId: Int,
        @Column var name: String,
        @Column var icon: String?,
        @Column var url: String?,
        @Column var views: Int,
        @Column var markedAsSpam: Boolean
    ) : Identifiable {
        constructor() : this(-1, -1, "", null, null, 0, false)

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }
}
