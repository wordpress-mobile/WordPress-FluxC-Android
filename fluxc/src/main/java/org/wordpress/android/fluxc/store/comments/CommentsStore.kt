package org.wordpress.android.fluxc.store.comments

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentsRestClient
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentXMLRPCClient
import org.wordpress.android.fluxc.persistence.comments.CommentsDao
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.INVALID_INPUT
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentsStore
@Inject constructor(
    private val commentsRestClient: CommentsRestClient,
    private val commentXMLRPCClient: CommentXMLRPCClient,
    private val commentsDao: CommentsDao,
    private val commentsMapper: CommentsMapper,
)
{
    data class CommentsActionPayload<T>(
        val response: T? = null
    ) : Payload<CommentError>() {
        constructor(error: CommentError) : this() {
            this.error = error
        }
    }

    data class ModeratedCommentInfo(val commentId: Long)


    suspend fun fetchComments(
        site: SiteModel,
        number: Int,
        offset: Int,
        networkStatusFilter: CommentStatus,
        cacheStatuses: List<CommentStatus>,
    ): CommentsActionPayload<List<CommentEntity>> {
        val payload = commentsRestClient.fetchComments(
                site = site,
                number = number,
                offset = offset,
                status = networkStatusFilter
        )

        return if (payload.isError) {
            CommentsActionPayload(payload.error)
        } else {
            val comments = payload.response?.comments?.mapNotNull {
                commentsMapper.commentDtoToEntity(it, site)
            } ?: listOf()

            commentsDao.appendOrOverwriteComments(
                    overwrite = offset == 0,
                    siteId = site.siteId,
                    statuses = cacheStatuses.map { it.toString() },
                    comments = comments
            )

            CommentsActionPayload(comments)
        }
    }

    suspend fun moderateCommentLocally(site: SiteModel, remoteCommentId: Long, newStatus: CommentStatus): CommentsActionPayload<ModeratedCommentInfo> {
        val comments = commentsDao.getCommentBySiteAndRemoteId(site.siteId, remoteCommentId)

        if (comments.isEmpty()) {
            return CommentsActionPayload(CommentError(CommentErrorType.UNKNOWN_COMMENT, "Unknown comment while moderating [site=${site.siteId} remoteCommentId=$remoteCommentId]"))
        }

        if (comments.size > 1) {
            return CommentsActionPayload(CommentError(CommentErrorType.DUPLICATE_COMMENT, "Duplicated comment while moderating [site=${site.siteId} remoteCommentId=$remoteCommentId]"))
        }

        val comment = comments.first().copy(status = newStatus.toString())

        val id = commentsDao.insertOrUpdateComment(comment)

        return CommentsActionPayload(ModeratedCommentInfo(commentId = id))
    }

    suspend fun pushComment(site: SiteModel, remoteCommentId: Long): CommentsActionPayload<CommentEntity> {
        val comments = commentsDao.getCommentBySiteAndRemoteId(site.siteId, remoteCommentId)

        if (comments.isEmpty()) {
            return CommentsActionPayload(CommentError(CommentErrorType.UNKNOWN_COMMENT, "Unknown comment while pushing [site=${site.siteId} remoteCommentId=$remoteCommentId]"))
        }

        if (comments.size > 1) {
            return CommentsActionPayload(CommentError(CommentErrorType.DUPLICATE_COMMENT, "Duplicated comment while pushing [site=${site.siteId} remoteCommentId=$remoteCommentId]"))
        }

        val commentFromCache = comments.first()

        return if (site.isUsingWpComRestApi) {
            val payload = commentsRestClient.pushComment(site, commentFromCache)

            if (payload.isError) {
                CommentsActionPayload(payload.error)
            } else {
                payload.response?.let {
                    val comment = commentsMapper.commentDtoToEntity(it, site).copy(id = commentFromCache.id)
                    commentsDao.insertOrUpdateComment(comment)
                    CommentsActionPayload(comment)
                } ?: CommentsActionPayload(CommentError(INVALID_INPUT, ""))
            }
        } else {
            // TODOD: implement push for self-hosted
            CommentsActionPayload()
        }
    }
}
