package org.wordpress.android.fluxc.persistence

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.toDomainModel
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.OrdersDao

@RunWith(AndroidJUnit4::class)
class OrdersPersistenceIntegrationTest {
    private lateinit var sut: OrdersDao
    private lateinit var database: WCAndroidDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sut = database.ordersDao
    }

    @Test
    fun testCorrectMappingWhenRemoteValueIsNull(): Unit = runBlocking {
        val dto = sampleOrderDto.copy(shipping_lines = null)

        sut.insertOrUpdateOrder(dto.toDomainModel(localSiteId))

        val order = sut.getOrder(orderId, localSiteId)

        println(order!!.shippingLines)
        assertThat(order.shippingLines).isNotNull()
    }

    @Test
    fun testCorrectMappingWhenRemoteValueIsJsonNull(): Unit = runBlocking {
        val dto = sampleOrderDto.copy(shipping_lines = JsonNull.INSTANCE)

        sut.insertOrUpdateOrder(dto.toDomainModel(localSiteId))

        val order = sut.getOrder(orderId, localSiteId)

        println(order!!.shippingLines)
        assertThat(order.shippingLines).isNotNull()
    }

    @Test
    fun testCorrectMappingWhenRemoteValueIsNullString(): Unit = runBlocking {
        val dto = sampleOrderDto

        sut.insertOrUpdateOrder(dto.toDomainModel(localSiteId).copy(shippingLines = "null"))

        val order = sut.getOrder(orderId, localSiteId)

        println(order!!.shippingLines)
        assertThat(order.shippingLines).isNotNull()
    }

    @Test
    fun testCorrectMappingWhenRemoteValueIsNullCapitalizedString(): Unit = runBlocking {
        val dto = sampleOrderDto

        sut.insertOrUpdateOrder(dto.toDomainModel(localSiteId).copy(shippingLines = "NULL"))

        val order = sut.getOrder(orderId, localSiteId)

        println(order!!.shippingLines)
        assertThat(order.shippingLines).isNotNull()
    }

    companion object{
        val orderId = 123L
        val localSiteId = LocalId(345)
        val sampleOrderDto = OrderDto(id = orderId)
    }
}