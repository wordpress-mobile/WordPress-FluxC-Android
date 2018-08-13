package org.wordpress.android.fluxc.model.portfolio

import org.wordpress.android.fluxc.model.PostModel

data class PortfolioModel(
    val id: Int,
    val title: String,
    val status: PortfolioStatus,
    val projectTypes: List<Long>,
    val projectTags: List<String>,
    val featuredImage: Long,
    val format: String,
    val slug: String,
    val excerpt: String) {
    constructor(post: PostModel) : this(
            post.id,
            post.title,
            PortfolioStatus.fromPost(post),
            post.categoryIdList,
            post.tagNameList,
            post.featuredImageId,
            post.postFormat,
            post.slug,
            post.excerpt
    )
}
