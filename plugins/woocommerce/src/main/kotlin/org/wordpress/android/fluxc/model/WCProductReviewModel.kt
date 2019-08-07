package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.WCProductReviewModel.AvatarSize.Companion
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.collections.MutableMap.MutableEntry

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
@RawConstraints(
        "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE",
        "UNIQUE (REMOTE_PRODUCT_REVIEW_ID, LOCAL_SITE_ID) ON CONFLICT REPLACE"
)
data class WCProductReviewModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    companion object {
        private val json by lazy { Gson() }
    }

    @Column var localSiteId = 0
    @Column var remoteProductReviewId = 0L // The unique ID for this product review on the server
    @Column var remoteProductId = 0L
    @Column var dateCreated = ""
    @Column var status = ""

    @SerializedName("reviewer")
    @Column var reviewerName = ""
    @Column var reviewerEmail = ""
    @Column var review = ""
    @Column var rating = 0
    @Column var verified = false

    @SerializedName("reviewer_avatar_urls")
    @Column var reviewerAvatarsJson = ""

    val reviewerAvatarUrlBySize: Map<AvatarSize, String> by lazy {
        val result = mutableMapOf<AvatarSize, String>()
        if (reviewerAvatarsJson.isNotEmpty()) {
            json.fromJson(reviewerAvatarsJson, JsonElement::class.java).asJsonObject.entrySet().forEach {
                result[AvatarSize.getAvatarSizeForValue(it.key.toInt())] = it.value.asString
            }
        }
        result
    }

    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = this.id

    enum class AvatarSize(val size: Int) {
        SMALL(24), MEDIUM(48), LARGE(96);

        companion object {
            fun getAvatarSizeForValue(size: Int): AvatarSize {
                return when(size) {
                    24 -> SMALL
                    48 -> MEDIUM
                    96 -> LARGE
                    else -> MEDIUM
                }
            }
        }
    }
}
