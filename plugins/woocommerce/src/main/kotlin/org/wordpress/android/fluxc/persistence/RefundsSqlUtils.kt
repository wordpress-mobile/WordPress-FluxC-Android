package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.wellsql.generated.RefundsTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundsRestClient.RefundResponse

object RefundsSqlUtils {
    private val gson: Gson by lazy {
        val builder = GsonBuilder()
        builder.create()
    }

    fun insert(site: SiteModel, orderId: Long, data: RefundResponse) =
            insert(site, orderId, listOf(data))

    fun insert(site: SiteModel, orderId: Long, data: List<RefundResponse>) {
        data.forEach { item ->
            val json = gson.toJson(item)
            WellSql.delete(RefundsBuilder::class.java)
                .where()
                .equals(RefundsTable.LOCAL_SITE_ID, site.id)
                .equals(RefundsTable.ORDER_ID, orderId)
                .equals(RefundsTable.REFUND_ID, item.refundId)
                .endWhere()
                .execute()
            WellSql.insert(
                    RefundsBuilder(
                            localSiteId = site.id,
                            orderId = orderId,
                            refundId = item.refundId,
                            data = json
                    )
            ).execute()
        }
    }

    fun selectAllRefunds(
        site: SiteModel,
        orderId: Long
    ): List<RefundResponse> {
        val models = WellSql.select(RefundsBuilder::class.java)
                .where()
                .equals(RefundsTable.LOCAL_SITE_ID, site.id)
                .equals(RefundsTable.ORDER_ID, orderId)
                .endWhere()
                .asModel
        return models.map { gson.fromJson(it.data, RefundResponse::class.java) }
    }

    fun selectRefund(
        site: SiteModel,
        refundId: Long
    ): RefundResponse? {
        val model = WellSql.select(RefundsBuilder::class.java)
                .where()
                .equals(RefundsTable.LOCAL_SITE_ID, site.id)
                .equals(RefundsTable.REFUND_ID, refundId)
                .endWhere()
                .asModel
                .firstOrNull()
        return model?.let { gson.fromJson(it.data, RefundResponse::class.java) }
    }

    @Table(name = "Refunds")
    data class RefundsBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var orderId: Long,
        @Column var refundId: Long,
        @Column var data: String
    ) : Identifiable {
        constructor() : this(-1, -1, -1, -1, "")

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }
}
