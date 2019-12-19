package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.utils.DateUtils

class WCProductImageModel(val id: Long) {
    var dateCreated: String = ""
    var src: String = ""
    var alt: String = ""
    var name: String = ""

    companion object {
        fun fromMediaModel(media: MediaModel): WCProductImageModel {
            with(WCProductImageModel(media.mediaId)) {
                dateCreated = media.uploadDate ?: DateUtils.getCurrentDateString()
                src = media.url ?: ""
                alt = media.alt ?: ""
                name = media.fileName ?: ""
                return this
            }
        }
    }
}
