package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.WCMetaData

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
                value = JsonPrimitive("internal value")
            ),
            WCMetaData(
                id = 2,
                key = "valid key",
                value = JsonPrimitive("valid value")
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
        val result: List<WCMetaData> = sut(orderDtoMock)

        // Then
        assertThat(result).isEqualTo(
            listOf(
                WCMetaData(
                    id = 2L,
                    key = "valid key",
                    value = JsonPrimitive("valid value")
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
        val result = sut(orderDtoMock)

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
        val result = sut(orderDtoMock)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `when Metadata value contains JSON, then remove it from the list`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(
                id = 1L,
                key = "key",
                value = JsonObject()
            ),
            WCMetaData(
                id = 2,
                key = "valid key",
                value = JsonPrimitive("valid value")
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
        val result = sut(orderDtoMock)

        // Then
        assertThat(result).isEqualTo(
            listOf(
                WCMetaData(
                    id = 2L,
                    key = "valid key",
                    value = JsonPrimitive("valid value")
                )
            )
        )
    }

    @Test
    fun `when Metadata key is invalid, then remove it from the list`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(
                id = 2L,
                key = "_internal key",
                value = JsonPrimitive("valid value")
            ),
            WCMetaData(
                id = 3L,
                key = "valid key",
                value = JsonPrimitive("valid value")
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
        val result = sut(orderDtoMock)

        // Then
        assertThat(result).isEqualTo(
            listOf(
                WCMetaData(
                    id = 3L,
                    key = "valid key",
                    value = JsonPrimitive("valid value")
                )
            )
        )
    }
}
