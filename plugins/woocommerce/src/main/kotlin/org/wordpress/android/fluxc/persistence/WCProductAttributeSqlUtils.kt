package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCProductAttributeModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeModel

object WCProductAttributeSqlUtils {
    fun getCurrentAttributes(siteID: Int) =
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

    fun insertSingleAttribute(attribute: WCProductAttributeModel) = attribute.apply {
        WellSql.insert(attribute)
                .asSingleTransaction(true)
                .execute()
    }

    fun deleteSingleStoredAttribute(attribute: WCProductAttributeModel, siteID: Int) = attribute.apply {
        WellSql.delete(WCProductAttributeModel::class.java)
                .where()
                .equals(WCProductAttributeModelTable.ID, attribute.id)
                .equals(WCProductAttributeModelTable.LOCAL_SITE_ID, siteID)
                .endWhere()
                .execute()
    }

    fun updateSingleStoredAttribute(attribute: WCProductAttributeModel, siteID: Int) = attribute.apply {
        WellSql.update(WCProductAttributeModel::class.java)
                .where()
                .equals(WCProductAttributeModelTable.ID, attribute.id)
                .equals(WCProductAttributeModelTable.LOCAL_SITE_ID, siteID)
                .endWhere()
                .execute()
    }
}
