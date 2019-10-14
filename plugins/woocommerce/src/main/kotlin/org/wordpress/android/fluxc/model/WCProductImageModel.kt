package org.wordpress.android.fluxc.model

class WCProductImageModel(val id: Long) {
    var dateCreated: String = ""
    var src: String = ""
    var alt: String = ""

    companion object {
        fun fromMediaModel(media: MediaModel): WCProductImageModel {
            with(WCProductImageModel(media.mediaId)) {
                dateCreated = media.uploadDate
                src = media.url
                alt = media.alt
                return this
            }
        }
    }
}
