package org.wordpress.android.fluxc.model.order.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import org.wordpress.android.fluxc.model.WCOrderModel

@Entity(
        foreignKeys = [ForeignKey(
                entity = WCOrderModel::class,
                parentColumns = ["localSiteId", "remoteOrderId"],
                childColumns = ["parentOrderLocalSiteId", "parentOrderId"],
                onDelete = ForeignKey.CASCADE
        )],
        primaryKeys = ["parentOrderId", "parentOrderLocalSiteId"]
)
class LineItemEntity(
    val parentOrderId: Long,
    val parentOrderLocalSiteId: Long,
    val id: Long? = null,
    val name: String? = null,
    val parentName: String? = null,
    var productId: Long? = null,
    val variationId: Long? = null,
    var quantity: Float? = null,
    val subtotal: String? = null,
    val total: String? = null, // Price x quantity
    val totalTax: String? = null,
    val sku: String? = null,
    val price: String? = null, // The per-item price
)
