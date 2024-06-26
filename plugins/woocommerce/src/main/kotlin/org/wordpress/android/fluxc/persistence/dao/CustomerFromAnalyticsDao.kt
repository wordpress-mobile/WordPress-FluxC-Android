package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.CustomerFromAnalyticsEntity

@Dao
abstract class CustomerFromAnalyticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCustomersFromAnalytics(customers: List<CustomerFromAnalyticsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCustomerFromAnalytics(customers: CustomerFromAnalyticsEntity)

    @Query("SELECT * FROM CustomerFromAnalytics WHERE localSiteId = :localSiteId AND id = :analyticCustomerId")
    abstract suspend fun getCustomerByAnalyticCustomerId(
        localSiteId: LocalOrRemoteId.LocalId,
        analyticCustomerId: Long
    ): CustomerFromAnalyticsEntity?
}
