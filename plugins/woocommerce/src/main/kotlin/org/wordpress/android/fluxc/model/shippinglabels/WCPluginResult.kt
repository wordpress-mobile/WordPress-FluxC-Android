package org.wordpress.android.fluxc.model.shippinglabels

data class WCPluginResult(
    val isPluginInstalled: Boolean,
    val isPluginActive: Boolean,
    val name: String?,
    val slug: String?,
    val version: String?
)
