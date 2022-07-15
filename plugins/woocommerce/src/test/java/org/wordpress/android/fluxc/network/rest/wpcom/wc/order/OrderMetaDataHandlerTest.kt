package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.persistence.dao.OrderMetaDataDao

class OrderMetaDataHandlerTest {
    private lateinit var sut: OrderMetaDataHandler
    private lateinit var orderDtoMock: OrderDto
    private lateinit var gsonMock: Gson
    private val orderMetaDataDaoMock = mock<OrderMetaDataDao>()

    @Before
    fun setUp() {
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

    @Test
    fun `when metadata contains internal keys, should remove all of them`() {
        // Given
        val rawMetadata = generateMetadata(amount = 100) {
            WCMetaData(
                id = it.toLong(),
                key = "_$it key",
                value = "$it value",
                displayKey = null,
                displayValue = null
            )
        }
        gsonMock = mock {
            val responseType = object : TypeToken<List<WCMetaData>>() {}.type
            on { fromJson<List<WCMetaData>?>(orderDtoMock.meta_data, responseType) }.thenReturn(
                rawMetadata
            )
        }
        sut = OrderMetaDataHandler(gsonMock, orderMetaDataDaoMock)

        // When
        sut(orderDtoMock, LocalId(1))

        // Then
        verify(orderMetaDataDaoMock).updateOrderMetaData(
            orderId = 1,
            localSiteId = LocalId(1),
            metaData = emptyList()
        )
    }

    private fun generateMetadata(
        amount: Int,
        constructor: (Int) -> WCMetaData
    ) = (1..amount).map { constructor(it) }
}
