package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCAttributeTermModelTable
import com.wellsql.generated.WCProductAttributeModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeModel
import org.wordpress.android.fluxc.model.product.attributes.terms.WCAttributeTermModel

object WCProductAttributeSqlUtils {
    fun getCurrentAttributes(siteID: Int) =
            WellSql.select(WCProductAttributeModel::class.java)
                    .where()
                    .equals(WCProductAttributeModelTable.LOCAL_SITE_ID, siteID)
                    .endWhere()
                    .asModel
                    ?.toList()
                    .orEmpty()

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
                .put(attribute)
                .execute()
    }

    fun getTerm(siteID: Int, termID: Int) =
            WellSql.select(WCAttributeTermModel::class.java)
                    .where()
                    .equals(WCAttributeTermModelTable.ID, termID)
                    .equals(WCAttributeTermModelTable.LOCAL_SITE_ID, siteID)
                    .endWhere()
                    .asModel
                    ?.toList()
                    ?.firstOrNull()

    fun insertAttributeTermsFromScratch(
        attributeID: Int,
        siteID: Int,
        terms: List<WCAttributeTermModel>
    ) {
        deleteAllTermsFromSingleAttribute(attributeID, siteID)
        WellSql.insert(terms)
                .asSingleTransaction(true)
                .execute()
    }

    private fun deleteCompleteAttributesList(siteID: Int) =
            WellSql.delete(WCProductAttributeModel::class.java)
                    .where()
                    .equals(WCProductAttributeModelTable.LOCAL_SITE_ID, siteID)
                    .endWhere()
                    .execute()

    private fun deleteAllTermsFromSingleAttribute(attributeID: Int, siteID: Int) =
            WellSql.delete(WCAttributeTermModel::class.java)
                    .where()
                    .equals(WCAttributeTermModelTable.LOCAL_SITE_ID, siteID)
                    .equals(WCAttributeTermModelTable.ATTRIBUTE_ID, attributeID)
                    .endWhere()
                    .execute()
}
