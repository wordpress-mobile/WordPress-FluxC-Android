package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.persistence.dao.OrderMetaDataDao
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity

class OrderMetaDataHandlerTest {
    private lateinit var sut: OrderMetaDataHandler
    private lateinit var orderDtoMock: OrderDto
    private lateinit var gsonMock: Gson
    private val orderMetaDataDaoMock = mock<OrderMetaDataDao>()
    private val jsonObjectMock = mock<JsonObject>()

    @Before
    fun setUp() {
        configureMocks()
        sut = OrderMetaDataHandler(gsonMock, orderMetaDataDaoMock)
    }

    private fun configureMocks() {
        orderDtoMock = mock {
            on { id }.thenReturn(1)
            on { meta_data }.thenReturn(jsonObjectMock)
        }
        gsonMock = mock {
            val responseType = object : TypeToken<List<WCMetaData>>() {}.type
            on { fromJson<List<WCMetaData>?>(jsonObjectMock, responseType) }.thenReturn(
                listOf()
            )
        }
    }

    @Test
    fun `when metadata contains internal keys, should remove all of them`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(
                id = 1,
                key = "_internal key",
                value = "internal value",
                displayKey = null,
                displayValue = null
            ),
            WCMetaData(
                id = 2,
                key = "valid key",
                value = "valid value",
                displayKey = null,
                displayValue = null
            )
        )
        gsonMock = mock {
            val responseType = object : TypeToken<List<WCMetaData>>() {}.type
            on { fromJson<List<WCMetaData>?>(jsonObjectMock, responseType) }.thenReturn(
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
            metaData = listOf(
                OrderMetaDataEntity(
                    id = 2L,
                    orderId = 1,
                    localSiteId = LocalId(1),
                    key = "valid key",
                    value = "valid value"
                )
            )
        )
    }

    @Test
    fun `when orderDto has no id, then cancel the operation`() {
        // Given
        orderDtoMock = mock {
            on { id }.thenReturn(null)
        }

        // When
        sut(orderDtoMock, LocalId(1))

        // Then
        verify(orderMetaDataDaoMock, never()).updateOrderMetaData(
            orderId = any(),
            localSiteId = any(),
            metaData = any()
        )
    }

    @Test
    fun `when JSON parsing fails, then set an empty metadata list`() {
        // Given
        gsonMock = mock {
            val responseType = object : TypeToken<List<WCMetaData>>() {}.type
            on { fromJson<List<WCMetaData>?>(jsonObjectMock, responseType) }.thenThrow(
                IllegalStateException()
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

    @Test
    fun `when Metadata value contains HTML, should strip the HTML tags only and keep the rest`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(
                id = 1L,
                key = "key",
                value = "<a>value</a> with some <b>HTML</b>",
                displayKey = null,
                displayValue = null
            )
        )
        gsonMock = mock {
            val responseType = object : TypeToken<List<WCMetaData>>() {}.type
            on { fromJson<List<WCMetaData>?>(jsonObjectMock, responseType) }.thenReturn(
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
            metaData = listOf(
                OrderMetaDataEntity(
                    id = 1L,
                    orderId = 1,
                    localSiteId = LocalId(1),
                    key = "key",
                    value = "value with some HTML"
                )
            )
        )
    }

    @Test
    fun `when Metadata value contains JSON, then remove it from the list`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(
                id = 1L,
                key = "key",
                value = "{\"key\":\"value\"}",
                displayKey = null,
                displayValue = null
            ),
            WCMetaData(
                id = 2,
                key = "valid key",
                value = "valid value",
                displayKey = null,
                displayValue = null
            )
        )
        gsonMock = mock {
            val responseType = object : TypeToken<List<WCMetaData>>() {}.type
            on { fromJson<List<WCMetaData>?>(jsonObjectMock, responseType) }.thenReturn(
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
            metaData = listOf(
                OrderMetaDataEntity(
                    id = 2L,
                    orderId = 1,
                    localSiteId = LocalId(1),
                    key = "valid key",
                    value = "valid value"
                )
            )
        )
    }

    @Test
    fun `when Metadata value is empty, then remove it from the list`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(
                id = 1L,
                key = "key",
                value = "",
                displayKey = null,
                displayValue = null
            ),
            WCMetaData(
                id = 2,
                key = "valid key",
                value = "valid value",
                displayKey = null,
                displayValue = null
            )
        )
        gsonMock = mock {
            val responseType = object : TypeToken<List<WCMetaData>>() {}.type
            on { fromJson<List<WCMetaData>?>(jsonObjectMock, responseType) }.thenReturn(
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
            metaData = listOf(
                OrderMetaDataEntity(
                    id = 2L,
                    orderId = 1,
                    localSiteId = LocalId(1),
                    key = "valid key",
                    value = "valid value"
                )
            )
        )
    }
}
