package org.wordpress.android.fluxc.model.gateways

data class WCGatewayModel(
    val id: String,
    val title: String,
    val description: String,
    val order: Int,
    val enabled: Boolean,
    val methodTitle: String,
    val methodDescription: String,
    val features: List<String>
)
