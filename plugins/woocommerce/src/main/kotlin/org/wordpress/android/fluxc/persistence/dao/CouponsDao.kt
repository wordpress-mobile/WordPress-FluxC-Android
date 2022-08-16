package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails

@Dao
interface CouponsDao {
    @Transaction
    @Query("SELECT * FROM Coupons WHERE siteId = :siteId ORDER BY dateCreated DESC")
    fun observeCoupons(siteId: Long): Flow<List<CouponWithEmails>>

    @Transaction
    @Query("SELECT * FROM Coupons " +
        "WHERE siteId = :siteId AND id IN (:couponIds) ORDER BY dateCreated DESC")
    suspend fun getCoupons(siteId: Long, couponIds: List<Long>): List<CouponWithEmails>

    @Transaction
    @Query("SELECT * FROM Coupons WHERE siteId = :siteId AND id = :couponId")
    fun observeCoupon(siteId: Long, couponId: Long): Flow<CouponWithEmails?>

    @Transaction
    @Query("SELECT * FROM Coupons WHERE siteId = :siteId AND id = :couponId")
    suspend fun getCoupon(siteId: Long, couponId: Long): CouponWithEmails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCoupon(entity: CouponEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCouponEmail(entity: CouponEmailEntity): Long

    @Query("DELETE FROM Coupons WHERE siteId = :siteId AND id = :couponId")
    suspend fun deleteCoupon(siteId: Long, couponId: Long)

    @Query("DELETE FROM Coupons WHERE siteId = :siteId")
    suspend fun deleteAllCoupons(siteId: Long)

    @Query("SELECT COUNT(*) FROM CouponEmails WHERE siteId = :siteId AND couponId = :couponId")
    suspend fun getEmailCount(siteId: Long, couponId: Long): Int
}
