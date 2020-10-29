package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCPluginsTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.plugins.WooPluginRestClient.FetchPluginsResponse.PluginModel

object WCPluginSqlUtils {
    fun insertOrUpdate(data: List<WCPluginModel>) {
        data.forEach {
            WellSql.delete(WCPluginModel::class.java)
                    .where()
                    .equals(WCPluginsTable.LOCAL_SITE_ID, it.localSiteId)
                    .equals(WCPluginsTable.SLUG, it.slug)
                    .endWhere()
                    .execute()
        }

        WellSql.insert(data).execute()
    }

    fun selectAll(site: SiteModel): List<WCPluginModel> {
        return WellSql.select(WCPluginModel::class.java)
                .where()
                .equals(WCPluginsTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel
    }

    fun selectSingle(site: SiteModel, slug: String): WCPluginModel? {
        return WellSql.select(WCPluginModel::class.java)
                .where()
                .equals(WCPluginsTable.LOCAL_SITE_ID, site.id)
                .equals(WCPluginsTable.SLUG, slug)
                .endWhere()
                .asModel
                .firstOrNull()
    }

    @Table(name = "WCPlugins", addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
    data class WCPluginModel(
        @PrimaryKey @Column private var id: Int = -1,
        @Column var localSiteId: Int = -1,
        @Column var active: Boolean = false,
        @Column var displayName: String = "",
        @Column var slug: String = "",
        @Column var version: String = ""
    ) : Identifiable {
        constructor(site: SiteModel, networkModel: PluginModel) : this(
                localSiteId = site.id,
                active = networkModel.isActive,
                displayName = networkModel.displayName,
                slug = networkModel.slug,
                version = networkModel.version
        )

        override fun getId() = id

        override fun setId(id: Int) {
            this.id = id
        }
    }
}
