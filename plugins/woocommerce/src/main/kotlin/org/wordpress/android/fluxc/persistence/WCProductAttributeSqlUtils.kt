package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCProductAttributeModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeModel

object WCProductAttributeSqlUtils {
    fun getCompleteAttributesList(siteID: Int) =
            WellSql.select(WCProductAttributeModel::class.java)
                    .where()
                    .equals(WCProductAttributeModelTable.LOCAL_SITE_ID, siteID)
                    .endWhere()
                    .asModel
                    ?.toList()
                    .orEmpty()

    fun deleteCompleteAttributesList(siteID: Int) =
            WellSql.delete(WCProductAttributeModel::class.java)
                    .where()
                    .equals(WCProductAttributeModelTable.LOCAL_SITE_ID, siteID)
                    .endWhere()
                    .execute()

    fun insertFromScratchCompleteAttributesList(attributes: List<WCProductAttributeModel>, siteID: Int) {
        deleteCompleteAttributesList(siteID)
        WellSql.insert(attributes)
                .asSingleTransaction(true).execute()
    }
}
