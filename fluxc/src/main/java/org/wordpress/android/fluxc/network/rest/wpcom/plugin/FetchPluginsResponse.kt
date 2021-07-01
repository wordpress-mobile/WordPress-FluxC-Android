package org.wordpress.android.fluxc.network.rest.wpcom.plugin

import com.google.gson.annotations.SerializedName

data class FetchPluginsResponse(
    @JvmField val plugins: List<PluginWPComRestResponse>? = null,
    @SerializedName("file_mod_capabilities") val fileModCapabilities: FileModCapabilities? = null
) {
    data class FileModCapabilities(
        @SerializedName("modify_files") val canModify: Boolean = false,
        @SerializedName("autoupdate_files") val canAutoUpdate: Boolean = false
    )
}
