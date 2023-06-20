package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.assertj.core.api.Assertions
import org.junit.Test
import org.wordpress.android.fluxc.model.StripProductMetaData
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.utils.JsonStringUtils

class StripProductMetaDataTest {
    private val gson = Gson()
    private val sut = StripProductMetaData(gson)


    @Test
    fun `when metadata contains not supported keys, then NOT supported keys are stripped`() {
        val result = sut.invoke(notOnlySupportedMetadata)
        val jsonResult = gson.fromJson(result, JsonArray::class.java)

        jsonResult.map { it as? JsonObject }.forEach { jsonObject ->
            Assertions.assertThat(jsonObject).isNotNull
            Assertions.assertThat(jsonObject?.get(WCMetaData.KEY)?.asString)
                .isIn(StripProductMetaData.SUPPORTED_KEYS)
        }
    }

    @Test
    fun `when metadata contains a supported key with a NULL value, then strip the supported key`() {
        val supportedKey = StripProductMetaData.SUPPORTED_KEYS.first()
        val value: String? = null

        val metadata = getOneItemMetadata(supportedKey, value)
        val result = sut.invoke(metadata)

        Assertions.assertThat(result).isEqualTo(JsonStringUtils.EMPTY.ARRAY)
    }

    @Test
    fun `when metadata contains a supported key with a EMPTY value, then strip the supported key`() {
        val supportedKey = StripProductMetaData.SUPPORTED_KEYS.first()
        val value = ""

        val metadata = getOneItemMetadata(supportedKey, value)
        val result = sut.invoke(metadata)

        Assertions.assertThat(result).isEqualTo(JsonStringUtils.EMPTY.ARRAY)
    }

    @Test
    fun `when metadata contains a supported key with a NOT EMPTY value, then value is kept`() {
        val supportedKey = StripProductMetaData.SUPPORTED_KEYS.first()
        val value = "valid"

        val metadata = getOneItemMetadata(supportedKey, value)
        val result = sut.invoke(metadata)

        val jsonResult = gson.fromJson(result, JsonArray::class.java)
        val resultItem = jsonResult[0] as JsonObject
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(resultItem[WCMetaData.KEY].asString).isEqualTo(supportedKey)
        Assertions.assertThat(resultItem[WCMetaData.VALUE].asString).isEqualTo(value)
    }

    @Test
    fun `when metadata is null, then empty array is return`() {
        val result = sut.invoke(null)
        Assertions.assertThat(result).isEqualTo(JsonStringUtils.EMPTY.ARRAY)
    }

    private fun getOneItemMetadata(itemKey: String, itemValue: String?): String {
        val item = JsonObject().apply {
            addProperty(WCMetaData.KEY, itemKey)
            itemValue?.let {
                addProperty(WCMetaData.VALUE, it)
            } ?: add(WCMetaData.VALUE, null)
        }
        val jsonArray = JsonArray().apply {
            add(item)
        }

        return gson.toJson(jsonArray)
    }

    private val notOnlySupportedMetadata = """
        [
          {
            "id": 11749,
            "key": "_wpcom_is_markdown",
            "value": "1"
          },
          {
            "id": 11750,
            "key": "_last_editor_used_jetpack",
            "value": "classic-editor"
          },
          {
            "id": 11754,
            "key": "_product_addons",
            "value": [{
                    "id": 11773,
                    "key": "_wc_gla_sync_status",
                    "value": "synced"
                  },
                  {
                    "id": 11774,
                    "key": "group_of_quantity",
                    "value": ""
                  }]
          },
          {
            "id": 11755,
            "key": "_product_addons_exclude_global",
            "value": "1"
          },
          {
            "id": 11772,
            "key": "_wc_gla_visibility",
            "value": "sync-and-show"
          },
          {
            "id": 11773,
            "key": "_wc_gla_sync_status",
            "value": "synced"
          },
          {
            "id": 11774,
            "key": "group_of_quantity",
            "value": ""
          },
          {
            "id": 11775,
            "key": "minimum_allowed_quantity",
            "value": ""
          },
          {
            "id": 11776,
            "key": "maximum_allowed_quantity",
            "value": ""
          },
          {
            "id": 11777,
            "key": "minmax_do_not_count",
            "value": "no"
          },
          {
            "id": 11778,
            "key": "minmax_cart_exclude",
            "value": "no"
          },
          {
            "id": 11779,
            "key": "minmax_category_group_of_exclude",
            "value": "no"
          },
          {
            "id": 11780,
            "key": "_wcsatt_disabled",
            "value": "yes"
          },
          {
            "id": 11781,
            "key": "_subscription_one_time_shipping",
            "value": "no"
          },
          {
            "id": 11782,
            "key": "_wcsatt_force_subscription",
            "value": "no"
          },
          {
            "id": 11785,
            "key": "_subscription_downloads_ids",
            "value": ""
          },
          {
            "id": 11786,
            "key": "minimum_allowed_quantity",
            "value": "40"
          },
          {
            "id": 11787,
            "key": "_wc_pre_orders_fee",
            "value": ""
          },
          {
            "id": 11788,
            "key": "_wpas_done_all",
            "value": "1"
          },
          {
            "id": 11909,
            "key": "_wc_gla_mc_status",
            "value": "not_synced"
          },
          {
            "id": 12406,
            "key": "_wc_gla_synced_at",
            "value": "1686193267"
          },
          {
            "id": 12407,
            "key": "_wc_gla_google_ids",
            "value": {
              "US": "online:en:US:gla_418"
            }
          },
          {
            "key": "_satt_data",
            "value": {
              "subscription_schemes": []
            }
          }
        ]
    """.trimIndent()
}
