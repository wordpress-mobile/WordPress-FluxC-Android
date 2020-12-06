package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCLocationsTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.data.WCLocationModel

object WCCountriesSqlUtils {
    fun getCountries(): List<WCLocationModel> {
        return WellSql.select(WCLocationModel::class.java)
                .where()
                .equals(WCLocationsTable.PARENT_CODE, null)
                .endWhere()
                .asModel
    }

    fun getStates(country: String): List<WCLocationModel> {
        return WellSql.select(WCLocationModel::class.java)
                .where()
                .equals(WCLocationsTable.PARENT_CODE, country)
                .endWhere()
                .asModel
    }

    fun insertOrUpdateLocations(locations: List<WCLocationModel>): Int {
        var rowsAffected = 0
        locations.forEach {
            rowsAffected += insertOrUpdateLocation(it)
        }
        return rowsAffected
    }

    fun insertOrUpdateLocation(location: WCLocationModel): Int {
        val result = WellSql.select(WCLocationModel::class.java)
                .where()
                .equals(WCLocationsTable.CODE, location.code)
                .equals(WCLocationsTable.PARENT_CODE, location.parentCode)
                .endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            // Insert
            WellSql.insert(location).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = result.id
            WellSql.update(WCLocationModel::class.java).whereId(oldId)
                    .put(location, UpdateAllExceptId(WCLocationModel::class.java)).execute()
        }
    }
}
