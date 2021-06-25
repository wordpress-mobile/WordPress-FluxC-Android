package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.RawEditorSettingsTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel

class RawEditorSettingsSqlUtils {
    fun replaceRawEditorSettingsForSite(site: SiteModel, rawEditorSettings: String?) {
        deleteRawEditorSettingsForSite(site)
        if (rawEditorSettings == null) return
        insertRawEditorSettingsForSite(site, rawEditorSettings)
    }

    fun getRawEditorSettingsForSite(site: SiteModel): String {
        return WellSql.select(RawEditorSettingsBuilder::class.java)
                .limit(1)
                .where()
                .equals(RawEditorSettingsTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel
                .firstOrNull()
                ?.rawEditorSettings ?: "{}";
    }

    fun deleteRawEditorSettingsForSite(site: SiteModel) {
        WellSql.delete(RawEditorSettingsBuilder::class.java)
                .where()
                .equals(RawEditorSettingsTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .execute()
    }

    private fun insertRawEditorSettingsForSite(site: SiteModel, rawEditorSettings: String?) {
        val rawEditorSettingsBuilder = RawEditorSettingsBuilder()
        rawEditorSettingsBuilder.localSiteId = site.id
        rawEditorSettingsBuilder.rawEditorSettings = rawEditorSettings
        WellSql.insert(rawEditorSettingsBuilder).execute()
    }

    @Table(name = "RawEditorSettings")
    data class RawEditorSettingsBuilder(@PrimaryKey @Column private var mId: Int = -1) : Identifiable {
        @Column var localSiteId: Int = -1
            @JvmName("getLocalSiteId")
            get
            @JvmName("setLocalSiteId")
            set
        @Column var rawEditorSettings: String? = null

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }
}
