package org.wordpress.android.fluxc.model.comments

import dagger.Reusable
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentWPComRestResponse
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.util.DateTimeUtils
import javax.inject.Inject

@Reusable
class CommentsMapper @Inject constructor() {
    fun commentDtoToEntity(commentDto: CommentWPComRestResponse, site: SiteModel): CommentEntity {
        return CommentEntity(
                //id = 0,
                remoteCommentId = commentDto.ID,
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                authorUrl = commentDto.author?.URL ?: "",
                authorName = commentDto.author?.name?.let {
                    StringEscapeUtils.unescapeHtml4(it)
                } ?: "",
                authorEmail = commentDto.author?.email?.let {
                    if ("false".equals(it)) {
                        it
                    } else {
                        ""
                    }
                } ?: "",
                authorProfileImageUrl = commentDto.author?.avatar_URL ?: "",
                remotePostId = commentDto.post?.ID ?: 0,
                postTitle = StringEscapeUtils.unescapeHtml4(commentDto.post?.title) ?: "",
                status = commentDto.status ?: "", // TODOD: this could be null; define a neutral status (ALL? maybe this is not a real status, so double check)
                datePublished = commentDto.date ?: "", // TODOD: define a neutral state for every nullable String
                publishedTimestamp = DateTimeUtils.timestampFromIso8601(commentDto.date),
                content = commentDto.content ?: "",
                url = commentDto.URL ?: "",
                remoteParentCommentId = commentDto.author?.ID ?: 0,
                hasParent = commentDto.parent != null,
                parentId = commentDto.parent?.ID ?: 0,
                iLike = commentDto.i_like
        )
    }
}
