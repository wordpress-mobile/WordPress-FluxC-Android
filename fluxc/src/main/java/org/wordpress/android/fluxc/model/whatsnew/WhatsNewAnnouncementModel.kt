package org.wordpress.android.fluxc.model.whatsnew

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
data class WhatsNewAnnouncementModel(
    val appVersionName: String,
    val announcementVersion: Int,
    val minimumAppVersion: String,
    val maximumAppVersion: String,
    val appVersionTargets: List<String>,
    val detailsUrl: String?,
    val isLocalized: Boolean,
    val responseLocale: String,
    val features: List<WhatsNewAnnouncementFeature>
) : Parcelable {
    @Parcelize
    @SuppressLint("ParcelCreator")
    data class WhatsNewAnnouncementFeature(
        val title: String?,
        val subtitle: String?,
        val icons: List<Icon>
    ) : Parcelable

    @Parcelize
    data class Icon(
        val iconUrl: String?,
        val iconType: IconType?,
    )

    @Parcelize
    enum class IconType {
        DARK,
        LIGHT,
        UNKNOWN
    }
}
