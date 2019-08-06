package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.collections.MutableMap.MutableEntry

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
@RawConstraints(
        "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE",
        "UNIQUE (REMOTE_PRODUCT_REVIEW_ID, LOCAL_SITE_ID) ON CONFLICT REPLACE"
)
data class WCProductReviewModel(@PrimaryKey @Column private var _id: Int = 0) : Identifiable {
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
    @Column var rating = 0
    @Column var verified = false

    @SerializedName("reviewer_avatar_urls")
    @Column var reviewerAvatarsJson = ""

    override fun setId(id: Int) {
        _id = id
    }

    override fun getId() = _id

    fun getReviewerAvatars(): List<ReviewerAvatar> {
        return if (reviewerAvatarsJson.isNotEmpty()) {
            val urls = mutableListOf<ReviewerAvatar>()
            val avatarsGson = json.fromJson(reviewerAvatarsJson, JsonElement::class.java)
            avatarsGson.asJsonObject.entrySet()
                    .forEach { mutableEntry: MutableEntry<String, JsonElement>? ->
                        mutableEntry?.let { avatarOption ->
                            val avatar = ReviewerAvatar(
                                    avatarOption.key.toInt(),
                                    avatarOption.value.toString()
                            )
                            urls.add(avatar)
                        }
                    }
            return urls
        } else {
            emptyList()
        }
    }

    data class ReviewerAvatar(val size: Int, val url: String)
}
