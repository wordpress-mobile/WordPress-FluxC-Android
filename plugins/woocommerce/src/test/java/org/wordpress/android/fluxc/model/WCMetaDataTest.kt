package org.wordpress.android.fluxc.model

import com.google.gson.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.WCMetaData.Companion.ID
import org.wordpress.android.fluxc.model.WCMetaData.Companion.KEY
import org.wordpress.android.fluxc.model.WCMetaData.Companion.VALUE

class WCMetaDataTest {
    @Test
    fun `when all required fields are present, should return a WCMetaData`() {
        val id = 1234L
        val key = "this_is_the_key"
        val value = "random value"

        val metadataJson = JsonObject().apply {
            addProperty(ID, id)
            addProperty(KEY, key)
            addProperty(VALUE, value)
        }

        val metadata = WCMetaData.fromJson(metadataJson)

        assertThat(metadata).isNotNull
        assertThat(metadata!!.id).isEqualTo(id)
        assertThat(metadata.key).isEqualTo(key)
        assertThat(metadata.value.stringValue).isEqualTo(value)
    }

    @Test
    fun `when ID is missing, should return null`() {
        val key = "this_is_the_key"
        val value = "random value"

        val metadataJson = JsonObject().apply {
            addProperty(KEY, key)
            addProperty(VALUE, value)
        }

        val metadata = WCMetaData.fromJson(metadataJson)

        assertThat(metadata).isNull()
    }

    @Test
    fun `when Key is missing, should return null`() {
        val id = 1234L
        val value = "random value"

        val metadataJson = JsonObject().apply {
            addProperty(ID, id)
            addProperty(VALUE, value)
        }

        val metadata = WCMetaData.fromJson(metadataJson)

        assertThat(metadata).isNull()
    }

    @Test
    fun `when Value is missing, should return null`() {
        val id = 1234L
        val key = "this_is_the_key"

        val metadataJson = JsonObject().apply {
            addProperty(ID, id)
            addProperty(KEY, key)
        }

        val metadata = WCMetaData.fromJson(metadataJson)

        assertThat(metadata).isNull()
    }
}
