package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.assertj.core.api.Assertions
import org.junit.Test

class OrdersBatchUpdateRequestTest {
    @Test
    fun `buildBody should create map with all available params`() {
        val body = OrdersBatchUpdateRequest.buildBody(
            createRequest = listOf(
                buildMap { put("createOrder", "value#create#1") }
            ),
            updateRequest = listOf(
                buildMap { put("updateOrder", "value#update#1") },
                buildMap { put("updateOrder", "value#update#2") }
            ),
            deleteRequest = listOf(1L, 2L)
        )

        Assertions.assertThat(
            body
        ).isEqualTo(
            buildMap {
                put(
                    "create",
                    listOf(
                        buildMap { put("createOrder", "value#create#1") }
                    )
                )
                put(
                    "update",
                    listOf(
                        buildMap { put("updateOrder", "value#update#1") },
                        buildMap { put("updateOrder", "value#update#2") }
                    )
                )
                put("delete", listOf(1L, 2L))
            }
        )
    }

    @Test
    fun `buildBody should not map values that do not contain order requests`() {
        val body = OrdersBatchUpdateRequest.buildBody(
            createRequest = emptyList(),
            updateRequest = emptyList(),
            deleteRequest = listOf(1L, 2L)
        )

        Assertions.assertThat(
            body
        ).isEqualTo(
            mapOf("delete" to listOf(1L, 2L))
        )
    }
}
