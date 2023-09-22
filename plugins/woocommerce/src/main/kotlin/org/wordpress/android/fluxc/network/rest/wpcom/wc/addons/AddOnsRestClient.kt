package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.dto.AddOnGroupDto
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class AddOnsRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchGlobalAddOnGroups(site: SiteModel): WooPayload<List<AddOnGroupDto>> {
        val url = WOOCOMMERCE.product_add_ons.pathV1Addons

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<AddOnGroupDto>::class.java
        )

        return response.toWooPayload { it.toList() }
    }

    suspend fun createGlobalAddonGroup(
        name: String,
        categoryIds: List<Long>,
        site: SiteModel
    ): WooPayload<AddOnGroupDto> {
        val url = WOOCOMMERCE.product_add_ons.pathV1Addons
        val body = buildMap {
            put("name", name)
            put("category_ids", categoryIds)
        }

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = body,
            clazz = AddOnGroupDto::class.java
        )

        return response.toWooPayload()
    }

    suspend fun updateGlobalAddonGroup(
        groupId: Long,
        name: String? = null,
        categoryIds: List<Long>? = null,
        site: SiteModel
    ): WooPayload<AddOnGroupDto> {
        val url = WOOCOMMERCE.product_add_ons.pathV1Addons
        val body = buildMap {
            put("id", groupId)
            name?.let { put("name", it) }
            categoryIds?.let { put("category_ids", it) }
        }

        val response = wooNetwork.executePutGsonRequest(
            site = site,
            path = url,
            body = body,
            clazz = AddOnGroupDto::class.java
        )

        return response.toWooPayload()
    }

    suspend fun deleteGlobalAddonGroup(
        groupId: Long,
        site: SiteModel
    ): WooPayload<AddOnGroupDto> {
        val url = WOOCOMMERCE.product_add_ons.pathV1Addons

        val response = wooNetwork.executeDeleteGsonRequest(
            site = site,
            path = url,
            params = mapOf("id" to groupId.toString()),
            clazz = AddOnGroupDto::class.java
        )

        return response.toWooPayload()
    }
}
