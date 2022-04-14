package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductCategoryEntity
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails

@Dao
abstract class CouponsDao {
    @Transaction
    @Query("SELECT * FROM Coupons WHERE siteId = :siteId")
    abstract fun observeCoupons(siteId: Long): Flow<List<CouponWithEmails>>

    @Transaction
    @Query("SELECT * FROM Coupons WHERE siteId = :siteId AND id IN (:couponIds)")
    abstract fun getCoupons(siteId: Long, couponIds: List<Long>): List<CouponWithEmails>

    @Transaction
    @Query("SELECT * FROM Coupons WHERE siteId = :siteId AND id = :couponId")
    abstract fun observeCoupon(siteId: Long, couponId: Long): Flow<CouponWithEmails?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCoupon(entity: CouponEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCouponAndProductCategory(
        entity: CouponAndProductCategoryEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCouponAndProduct(entity: CouponAndProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCouponEmail(entity: CouponEmailEntity): Long

    @Update
    abstract suspend fun updateCoupon(entity: CouponEntity)

    @Update
    abstract suspend fun updateCouponEmail(entity: CouponEmailEntity)

    @Update
    abstract suspend fun updateCouponAndProductCategory(entity: CouponAndProductCategoryEntity)

    @Update
    abstract suspend fun updateCouponAndProduct(entity: CouponAndProductEntity)

    @Transaction
    open suspend fun insertOrUpdateCoupon(entity: CouponEntity) {
        val id = insertCoupon(entity)
        if (id == -1L) {
            updateCoupon(entity)
        }
    }

    @Transaction
    open suspend fun insertOrUpdateCouponAndProductCategory(
        entity: CouponAndProductCategoryEntity
    ) {
        val id = insertCouponAndProductCategory(entity)
        if (id == -1L) {
            updateCouponAndProductCategory(entity)
        }
    }

    @Transaction
    open suspend fun insertOrUpdateCouponAndProduct(entity: CouponAndProductEntity) {
        val id = insertCouponAndProduct(entity)
        if (id == -1L) {
            updateCouponAndProduct(entity)
        }
    }

    @Transaction
    open suspend fun insertOrUpdateCouponEmail(entity: CouponEmailEntity) {
        val id = insertCouponEmail(entity)
        if (id == -1L) {
            updateCouponEmail(entity)
        }
    }

    @Query("DELETE FROM Coupons WHERE siteId = :siteId AND id = :couponId")
    abstract suspend fun deleteCoupon(siteId: Long, couponId: Long)

    @Query("DELETE FROM Coupons WHERE siteId = :siteId")
    abstract suspend fun deleteAllCoupons(siteId: Long)

    @Query("SELECT COUNT(*) FROM CouponEmails WHERE siteId = :siteId AND couponId = :couponId")
    abstract suspend fun getEmailCount(siteId: Long, couponId: Long): Int
}
