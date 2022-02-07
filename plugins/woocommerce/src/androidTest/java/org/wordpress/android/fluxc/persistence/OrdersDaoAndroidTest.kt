package org.wordpress.android.fluxc.persistence

import android.app.Application
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.persistence.dao.OrdersDao

@RunWith(AndroidJUnit4::class)
class OrdersDaoAndroidTest {
    private lateinit var sut: OrdersDao
    private lateinit var database: WCAndroidDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        sut = database.ordersDao()
    }

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val randString = (1..(100000))
            .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")

    @Test
    fun testBigOrderForSite() {
        runBlocking {
            val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID }

            val remoteOrderId = RemoteId(1)
            WCOrderModel(
                    remoteOrderId = remoteOrderId,
                    localSiteId = LocalId(TEST_LOCAL_SITE_ID),
                    status = CoreOrderStatus.PROCESSING.value,
                    lineItems = randString,
                    shippingLines = randString,
                    shippingCountry = randString,
                    shippingPhone = randString,
                    feeLines = randString,
                    taxLines = randString,
                    metaData = randString,
                    billingFirstName = randString,
                    billingLastName = randString,
                    billingCompany = randString,
                    billingAddress1 = randString,
                    billingAddress2 = randString,
                    billingCity = randString,
                    billingState = randString,
                    billingPostcode = randString,
                    billingCountry = randString,
                    billingEmail = randString,
                    billingPhone = randString,
                    shippingFirstName = randString,
                    shippingLastName = randString,
                    shippingCompany = randString,
                    shippingAddress1 = randString,
                    shippingAddress2 = randString,
                    shippingCity = randString,
                    shippingState = randString,
                    shippingPostcode = randString,
            ).also {
                sut.insertOrUpdateOrder(it)
            }
            val storedOrders = sut.getOrdersForSiteByRemoteIds(site.localId(), listOf(remoteOrderId))

            assertThat(storedOrders).hasSize(1)
        }
    }

    @Test
    fun testBigAmountOfOrderForSite() {
        runBlocking {
            val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID }

            for (id in 1..1000L) {
                val remoteOrderId = RemoteId(id)
                WCOrderModel(
                        remoteOrderId = remoteOrderId,
                        localSiteId = LocalId(TEST_LOCAL_SITE_ID),
                        status = CoreOrderStatus.PROCESSING.value,
                        lineItems = randString,
                        shippingLines = randString,
                        shippingCountry = randString,
                        shippingPhone = randString,
                        feeLines = randString,
                        taxLines = randString,
                        metaData = randString,
                        billingFirstName = randString,
                        billingLastName = randString,
                        billingCompany = randString,
                        billingAddress1 = randString,
                        billingAddress2 = randString,
                        billingCity = randString,
                        billingState = randString,
                        billingPostcode = randString,
                        billingCountry = randString,
                        billingEmail = randString,
                        billingPhone = randString,
                        shippingFirstName = randString,
                        shippingLastName = randString,
                        shippingCompany = randString,
                        shippingAddress1 = randString,
                        shippingAddress2 = randString,
                        shippingCity = randString,
                        shippingState = randString,
                        shippingPostcode = randString,
                ).also {
                    sut.insertOrUpdateOrder(it)
                }
            }

            Log.d("tag", "Test")
            val storedOrders = sut.getOrdersForSiteByRemoteIds(site.localId(), (1..100L).map { RemoteId(it) })

            assertThat(storedOrders).hasSize(100)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private companion object {
        const val TEST_LOCAL_SITE_ID = 6

    }
}
