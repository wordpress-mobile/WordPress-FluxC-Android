package org.wordpress.android.fluxc.model

import com.google.gson.JsonObject

data class WCProductFileModel(val id: String? = null, val name: String = "", val url: String = "") {
    fun toJson() = JsonObject().apply {
        if (id != null) addProperty("id", id)
        addProperty("name", name)
        addProperty("file", url)
    }
}