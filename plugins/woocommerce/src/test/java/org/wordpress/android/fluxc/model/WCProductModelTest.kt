package org.wordpress.android.fluxc.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.utils.AppLogWrapper

class WCProductModelTest {
    @Test
    fun `empty attributes json array return empty array`() {
        val attributes = JsonArray()

        val sut = WCProductModel().apply {
            this.attributes = attributes.toString()
        }

        val result = sut.getAttributeList()

        Assertions.assertThat(result).isEmpty()
    }

    @Test
    fun `json attributes without option return empty option`() {
        val attribute = JsonObject().apply {
            addProperty("id",1)
            addProperty("name", "attribute name")
            addProperty("variation", false)
            addProperty("visible", true)
        }
        val attributes = JsonArray().apply {
            add(attribute)
        }

        val sut = WCProductModel().apply {
            this.attributes = attributes.toString()
        }

        val result = sut.getAttributeList()

        Assertions.assertThat(result).isNotEmpty
    }

    @Test
    fun `json attributes as a primitive doesn't crash`() {
        val appLogWrapper: AppLogWrapper = mock()
        doNothing().`when`(appLogWrapper).e(any(), any())

        val attribute = JsonPrimitive(true)

        val attributes = JsonArray().apply { add(attribute) }

        val sut = WCProductModel(appLogWrapper = appLogWrapper).apply {
            this.attributes = attributes.toString()
        }

        val result = sut.getAttributeList()

        // If we can't process product's attributes then we return an empty list
        Assertions.assertThat(result).isEmpty()
    }

    @Test
    fun `json attributes empty doesn't crash`() {
        val appLogWrapper: AppLogWrapper = mock()
        doNothing().`when`(appLogWrapper).e(any(), any())

        val sut = WCProductModel(appLogWrapper = appLogWrapper).apply {
            this.attributes = ""
        }
        val result = sut.getAttributeList()

        // If we can't process product's attributes then we return an empty list
        Assertions.assertThat(result).isEmpty()
    }
}
