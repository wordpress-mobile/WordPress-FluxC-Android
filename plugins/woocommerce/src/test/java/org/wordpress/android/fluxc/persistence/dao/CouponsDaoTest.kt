package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import java.io.IOException
import org.assertj.core.api.Assertions.assertThat
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CouponsDaoTest {
    private lateinit var couponsDao: CouponsDao
    private lateinit var db: WCAndroidDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        couponsDao = db.couponsDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test coupon insert and update`(): Unit = runBlocking {
        // when
        var coupon = generateCouponEntity()
        val email = CouponEmailEntity(
            couponId = coupon.id,
            siteId = coupon.siteId,
            email = "test@test.com"
        )
        couponsDao.insertOrUpdateCoupon(coupon)
        couponsDao.insertOrUpdateCouponEmail(email)
        var observedCoupons = couponsDao.observeCoupons(coupon.siteId).first()

        // then
        val expected = CouponWithEmails(coupon, listOf(email))
        assertThat(observedCoupons.first()).isEqualTo(expected)

        // when
        coupon = coupon.copy(description = "Updated", usageLimit = 2)
        couponsDao.insertOrUpdateCoupon(coupon)
        observedCoupons = couponsDao.observeCoupons(coupon.siteId).first()

        // then
        assertThat(observedCoupons.first().couponEntity).isEqualTo(coupon)
    }

    @Test
    fun `test coupon delete`(): Unit = runBlocking {
        // when
        val coupon = generateCouponEntity()
        val email = CouponEmailEntity(
            couponId = coupon.id,
            siteId = coupon.siteId,
            email = "test@test.com"
        )
        couponsDao.insertOrUpdateCoupon(coupon)
        couponsDao.insertOrUpdateCouponEmail(email)
        var observedCoupons = couponsDao.observeCoupons(coupon.siteId).first()
        var emailCount = couponsDao.getEmailCount(coupon.siteId, coupon.id)

        // then
        assertThat(observedCoupons.size).isEqualTo(1)
        assertThat(emailCount).isEqualTo(1)

        // when
        couponsDao.deleteCoupon(coupon.siteId, coupon.id)
        observedCoupons = couponsDao.observeCoupons(coupon.siteId).first()
        emailCount = couponsDao.getEmailCount(coupon.siteId, coupon.id)

        // then
        assertThat(observedCoupons.size).isEqualTo(0)
        assertThat(emailCount).isEqualTo(0)
    }

    companion object {
        fun generateCouponEntity(id: Long = 0) = CouponEntity(
            id = id,
            siteId = 1,
            code = "code",
            dateCreated = "2007-04-05T14:30Z",
            dateCreatedGmt = "2007-04-05T14:30Z",
            dateModified = "2007-04-05T14:30Z",
            dateModifiedGmt = "2007-04-05T14:30Z",
            discountType = "percent",
            description = "Description",
            dateExpires = "2008-04-05T14:30Z",
            dateExpiresGmt = "2008-04-05T14:30Z",
            usageCount = 0,
            isForIndividualUse = true,
            usageLimit = 3,
            usageLimitPerUser = 1,
            limitUsageToXItems = 5,
            isShippingFree = false,
            areSaleItemsExcluded = true,
            minimumAmount = "10",
            maximumAmount = "100"
        )
    }
}
