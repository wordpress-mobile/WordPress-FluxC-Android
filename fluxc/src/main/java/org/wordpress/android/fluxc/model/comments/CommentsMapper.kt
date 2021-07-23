package org.wordpress.android.fluxc.model.comments

import dagger.Reusable
import org.apache.commons.text.StringEscapeUtils
import org.w3c.dom.Comment
import org.wordpress.android.fluxc.model.CommentModel
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
            authorUrl = commentDto.author?.URL,
            authorName = commentDto.author?.name?.let {
                StringEscapeUtils.unescapeHtml4(it)
            },
            authorEmail = commentDto.author?.email?.let {
                if ("false".equals(it)) {
                    ""
                } else {
                    it
                }
            },
            authorProfileImageUrl = commentDto.author?.avatar_URL,
            remotePostId = commentDto.post?.ID ?: 0,
            postTitle = StringEscapeUtils.unescapeHtml4(commentDto.post?.title),
            status = commentDto.status,
            datePublished = commentDto.date,
            publishedTimestamp = DateTimeUtils.timestampFromIso8601(commentDto.date),
            content = commentDto.content,
            url = commentDto.URL,
            remoteParentCommentId = commentDto.author?.ID ?: 0L,
            hasParent = commentDto.parent != null,
            parentId = commentDto.parent?.ID ?: 0,
            iLike = commentDto.i_like
        )
    }

    fun commentEntityToLegacyModel(entity: CommentEntity): CommentModel {
        return CommentModel().apply {
            this.id = entity.id.toInt()
            this.remoteCommentId = entity.remoteCommentId
            this.remotePostId = entity.remotePostId
            this.remoteParentCommentId = entity.remoteParentCommentId
            this.localSiteId = entity.localSiteId
            this.remoteSiteId = entity.remoteSiteId
            this.authorUrl = entity.authorUrl
            this.authorName = entity.authorName
            this.authorEmail = entity.authorEmail
            this.authorProfileImageUrl = entity.authorProfileImageUrl
            this.postTitle = entity.postTitle
            this.status = entity.status
            this.datePublished = entity.datePublished
            this.publishedTimestamp = entity.publishedTimestamp
            this.content = entity.content
            this.url = entity.url
            this.hasParent = entity.hasParent
            this.parentId = entity.parentId
            this.iLike = entity.iLike
        }
    }

    fun commentLegacyModelToEntity(commentModel: CommentModel): CommentEntity {
        return CommentEntity(
                id = commentModel.id.toLong(),
                remoteCommentId = commentModel.remoteCommentId,
                remotePostId = commentModel.remotePostId,
                remoteParentCommentId = commentModel.remoteParentCommentId,
                localSiteId = commentModel.localSiteId,
                remoteSiteId = commentModel.remoteSiteId,
                authorUrl = commentModel.authorUrl,
                authorName = commentModel.authorName,
                authorEmail = commentModel.authorEmail,
                authorProfileImageUrl = commentModel.authorProfileImageUrl,
                postTitle = commentModel.postTitle,
                status = commentModel.status,
                datePublished = commentModel.datePublished,
                publishedTimestamp = commentModel.publishedTimestamp,
                content = commentModel.content,
                url = commentModel.url,
                hasParent = commentModel.hasParent,
                parentId = commentModel.parentId,
                iLike = commentModel.iLike
        )
    }
}
