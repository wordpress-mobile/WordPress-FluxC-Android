package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat

class ProductApiResponseTest {
    private lateinit var sut: ProductApiResponse

    @Before
    fun setUp() {
        sut = ProductApiResponse()
    }

    @Test
    fun `should map total sales to zero when received value outreaches Long range`() {
        sut.total_sales = "${Long.MAX_VALUE}" + "1"

        assertThat(sut.asProductModel().totalSales).isZero
    }

    @Test
    fun `should map total sales value when received value is in Long range`() {
        sut.total_sales = "${Long.MAX_VALUE}"

        assertThat(sut.asProductModel().totalSales).isEqualTo(Long.MAX_VALUE)
    }

    @Test
    fun `should map total sales value to zero when received value is not a number`() {
        sut.total_sales = "not a number"

        assertThat(sut.asProductModel().totalSales).isZero
    }
}
