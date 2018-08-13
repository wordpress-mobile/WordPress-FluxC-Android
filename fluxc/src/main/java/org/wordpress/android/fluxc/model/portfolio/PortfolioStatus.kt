package org.wordpress.android.fluxc.model.portfolio

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus

enum class PortfolioStatus {
    UNKNOWN,
    PUBLISHED,
    DRAFT,
    TRASHED,
    SCHEDULED;

    companion object {
        fun fromPost(post: PostModel): PortfolioStatus {
            return fromPostStatus(PostStatus.fromPost(post))
        }

        fun fromPostStatus(status: PostStatus): PortfolioStatus {
            return when (status) {
                PostStatus.PUBLISHED -> PUBLISHED
                PostStatus.DRAFT -> DRAFT
                PostStatus.TRASHED -> TRASHED
                PostStatus.SCHEDULED -> SCHEDULED
                else -> UNKNOWN
            }
        }
    }
}
