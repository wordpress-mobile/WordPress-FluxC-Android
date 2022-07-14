package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.persistence.dao.OrderMetaDataDao

class OrderMetaDataHandlerTest {
    private lateinit var sut: OrderMetaDataHandler
    private lateinit var orderDtoMock: OrderDto
    private lateinit var gsonMock: Gson
    private val orderMetaDataDaoMock
        get() = mock<OrderMetaDataDao>()

    @Before
    private fun setUp() {
        configureMocks()
        sut = OrderMetaDataHandler(gsonMock, orderMetaDataDaoMock)
    }

    private fun configureMocks() {
        orderDtoMock = mock {
            on { id }.thenReturn(1)
        }
        gsonMock = mock {
            val responseType = object : TypeToken<List<WCMetaData>>() {}.type
            on { fromJson<List<WCMetaData>?>(orderDtoMock.meta_data, responseType) }.thenReturn(
                listOf()
            )
        }
    }
}
