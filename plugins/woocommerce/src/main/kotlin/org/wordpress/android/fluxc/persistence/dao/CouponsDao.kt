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
    @Query("SELECT * FROM Coupons WHERE siteId = :siteId ORDER BY dateCreated DESC")
    abstract fun observeCoupons(siteId: Long): Flow<List<CouponWithEmails>>

    @Transaction
    @Query("SELECT * FROM Coupons " +
        "WHERE siteId = :siteId AND id IN (:couponIds) ORDER BY dateCreated DESC")
    abstract fun getCoupons(siteId: Long, couponIds: List<Long>): List<CouponWithEmails>

    @Transaction
    @Query("SELECT * FROM Coupons WHERE siteId = :siteId AND id = :couponId")
    abstract fun observeCoupon(siteId: Long, couponId: Long): Flow<CouponWithEmails?>

    @Transaction
    @Query("SELECT * FROM Coupons WHERE siteId = :siteId AND id = :couponId")
    abstract fun getCoupon(siteId: Long, couponId: Long): CouponWithEmails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateCoupon(entity: CouponEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateCouponAndProductCategory(
        entity: CouponAndProductCategoryEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateCouponAndProduct(entity: CouponAndProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateCouponEmail(entity: CouponEmailEntity): Long

    @Query("DELETE FROM Coupons WHERE siteId = :siteId AND id = :couponId")
    abstract suspend fun deleteCoupon(siteId: Long, couponId: Long)

    @Query("DELETE FROM Coupons WHERE siteId = :siteId")
    abstract suspend fun deleteAllCoupons(siteId: Long)

    @Query("SELECT COUNT(*) FROM CouponEmails WHERE siteId = :siteId AND couponId = :couponId")
    abstract suspend fun getEmailCount(siteId: Long, couponId: Long): Int
}
