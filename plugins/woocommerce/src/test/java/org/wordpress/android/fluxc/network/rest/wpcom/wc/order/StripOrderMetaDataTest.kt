package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity

class StripOrderMetaDataTest {
    private lateinit var sut: StripOrderMetaData
    private lateinit var orderDtoMock: OrderDto
    private lateinit var gsonMock: Gson
    private val jsonObjectMock = mock<JsonObject>()

    @Before
    fun setUp() {
        configureMocks()
        sut = StripOrderMetaData(gsonMock)
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
        sut = StripOrderMetaData(gsonMock)

        // When
        val result: List<OrderMetaDataEntity> = sut(orderDtoMock, LocalId(1))

        // Then
        assertThat(result).isEqualTo(
            listOf(
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
        val result = sut(orderDtoMock, LocalId(1))

        // Then
        assertThat(result).isEmpty()
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
        sut = StripOrderMetaData(gsonMock)

        // When
        val result = sut(orderDtoMock, LocalId(1))

        // Then
        assertThat(result).isEmpty()
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
        sut = StripOrderMetaData(gsonMock)

        // When
        val result = sut(orderDtoMock, LocalId(1))

        // Then
        assertThat(result).isEqualTo(
            listOf(
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
        sut = StripOrderMetaData(gsonMock)

        // When
        val result = sut(orderDtoMock, LocalId(1))

        // Then
        assertThat(result).isEqualTo(
            listOf(
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
        sut = StripOrderMetaData(gsonMock)

        // When
        val result = sut(orderDtoMock, LocalId(1))

        // Then
        assertThat(result).isEqualTo(
            listOf(
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
    fun `when Metadata key is invalid, then remove it from the list`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(
                id = 1L,
                key = null,
                value = "valid value",
                displayKey = null,
                displayValue = null
            ),
            WCMetaData(
                id = 2L,
                key = "_internal key",
                value = "valid value",
                displayKey = null,
                displayValue = null
            ),
            WCMetaData(
                id = 3L,
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
        sut = StripOrderMetaData(gsonMock)

        // When
        val result = sut(orderDtoMock, LocalId(1))

        // Then
        assertThat(result).isEqualTo(
            listOf(
                OrderMetaDataEntity(
                    id = 3L,
                    orderId = 1,
                    localSiteId = LocalId(1),
                    key = "valid key",
                    value = "valid value"
                )
            )
        )
    }
}
