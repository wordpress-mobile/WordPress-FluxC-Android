package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductCategoryEntity
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails

@Dao
abstract class CouponsDao {
    @Transaction
    @Query("SELECT * FROM Coupons WHERE siteId = :siteId ORDER BY id")
    abstract fun observeCoupons(siteId: Long): Flow<List<CouponWithEmails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateCoupon(entity: CouponEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateCouponAndProductCategory(
        entity: CouponAndProductCategoryEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateCouponAndProduct(entity: CouponAndProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateCouponEmail(entity: CouponEmailEntity)
}
