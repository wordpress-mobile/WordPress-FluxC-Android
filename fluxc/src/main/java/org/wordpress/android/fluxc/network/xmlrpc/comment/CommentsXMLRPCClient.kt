package org.wordpress.android.fluxc.network.xmlrpc.comment

import com.android.volley.RequestQueue
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentsRestClient.CommentsApiPayload
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCUtils
import org.wordpress.android.fluxc.persistence.comments.CommentEntityList
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.utils.CommentErrorUtilsWrapper
import org.wordpress.android.util.DateTimeUtils
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CommentsXMLRPCClient @Inject constructor(
    dispatcher: Dispatcher?,
    @Named("custom-ssl") requestQueue: RequestQueue?,
    userAgent: UserAgent?,
    httpAuthManager: HTTPAuthManager?,
    private val commentErrorUtilsWrapper: CommentErrorUtilsWrapper,
    private val xmlrpcRequestBuilder: XMLRPCRequestBuilder
) : BaseXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager) {
    suspend fun fetchCommentsPage(
        site: SiteModel,
        number: Int,
        offset: Int,
        status: CommentStatus
    ): CommentsApiPayload<CommentEntityList> {
        val params: MutableList<Any> = ArrayList(4)

        val commentParams = mutableMapOf<String, Any>(
                "number" to number,
                "offset" to offset
        )

        if (status != CommentStatus.ALL) {
            commentParams["status"] = getXMLRPCCommentStatus(status)
        }

        params.add(site.selfHostedSiteId)
        params.add(site.username)
        params.add(site.password)
        params.add(commentParams)

        val response = xmlrpcRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.xmlRpcUrl,
                method = XMLRPC.GET_COMMENTS,
                params = params,
                clazz = Any::class.java, // TODOD: better check this!
        )

        return when(response) {
            is Success -> {
                CommentsApiPayload(commentsResponseToCommentList(response.data, site))
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun pushComment(site: SiteModel, comment: CommentEntity): CommentsApiPayload<CommentEntity> {
        val params: MutableList<Any> = ArrayList(5)

        val commentParams = mutableMapOf<String, Any?>(
                "content" to comment.content,
                "date" to comment.datePublished,
                "status" to getXMLRPCCommentStatus(CommentStatus.fromString(comment.status))
        )

        params.add(site.selfHostedSiteId)
        params.add(site.username)
        params.add(site.password)
        params.add(comment.remoteCommentId)
        params.add(commentParams)

        val response = xmlrpcRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.xmlRpcUrl,
                method = XMLRPC.EDIT_COMMENT,
                params = params,
                clazz = Any::class.java, // TODOD: better check this!
        )

        return when(response) {
            is Success -> {
                CommentsApiPayload(comment)
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun fetchComment(site: SiteModel, remoteCommentId: Long): CommentsApiPayload<CommentEntity> {
        val params: MutableList<Any> = ArrayList(4)

        params.add(site.selfHostedSiteId)
        params.add(site.username)
        params.add(site.password)
        params.add(remoteCommentId)

        val response = xmlrpcRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.xmlRpcUrl,
                method = XMLRPC.GET_COMMENT,
                params = params,
                clazz = Any::class.java, // TODOD: better check this!
        )

        return when(response) {
            is Success -> {
                CommentsApiPayload(commentResponseToComment(response.data, site))
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun deleteComment(site: SiteModel, remoteCommentId: Long, comment: CommentEntity?): CommentsApiPayload<CommentEntity?> {
        val params: MutableList<Any> = ArrayList(4)

        params.add(site.selfHostedSiteId)
        params.add(site.username)
        params.add(site.password)
        params.add(remoteCommentId)

        val response = xmlrpcRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.xmlRpcUrl,
                method = XMLRPC.DELETE_COMMENT,
                params = params,
                clazz = Any::class.java, // TODOD: better check this!
        )

        return when(response) {
            is Success -> {
                // This is ugly but the XMLRPC response doesn't contain any info about the updated comment.
                // TODOD: check in debug that response doesn't contain any info
                CommentsApiPayload(null)
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun createNewReply(site: SiteModel, comment: CommentEntity, reply: CommentEntity): CommentsApiPayload<CommentEntity>{
        val commentParams = mutableMapOf<String, Any?>(
                "content" to reply.content,
                "comment_parent" to comment.remoteCommentId,
        )

        if (reply.authorName != null) {
            commentParams["author"] = reply.authorName
        }

        if (reply.authorUrl != null) {
            commentParams["author_url"] = reply.authorUrl
        }

        if (reply.authorEmail != null) {
            commentParams["author_email"] = reply.authorEmail
        }

        return newComment(site, comment.remotePostId, reply, comment.remoteCommentId, commentParams)
    }

    suspend fun createNewComment(site: SiteModel, remotePostId: Long, comment: CommentEntity): CommentsApiPayload<CommentEntity> {
        val commentParams = mutableMapOf<String, Any?>(
                "content" to comment.content,
        )

        if (comment.remoteParentCommentId != 0L) {
            commentParams["comment_parent"] = comment.remoteParentCommentId;
        }
        if (comment.authorName != null) {
            commentParams["author"] = comment.authorName;
        }
        if (comment.authorUrl != null) {
            commentParams["author_url"] = comment.authorUrl;
        }
        if (comment.authorEmail != null) {
            commentParams["author_email"] = comment.authorEmail;
        }

        return newComment(site, remotePostId, comment, comment.remoteParentCommentId, commentParams)
    }

    private suspend fun newComment(site: SiteModel, remotePostId: Long, comment: CommentEntity, parentId: Long, commentParams: Map<String, Any?>): CommentsApiPayload<CommentEntity> {
        val params: MutableList<Any> = ArrayList(5)

        params.add(site.selfHostedSiteId)
        params.add(site.username)
        params.add(site.password)
        params.add(remotePostId)
        params.add(commentParams)

        val response = xmlrpcRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.xmlRpcUrl,
                method = XMLRPC.NEW_COMMENT,
                params = params,
                clazz = Any::class.java, // TODOD: better check this!
        )

        return when(response) {
            is Success -> {
                if (response.data is Int) {
                    val newComment = comment.copy(remoteParentCommentId = parentId, remoteCommentId = response.data.toLong())
                    CommentsApiPayload(newComment)
                } else {
                    val newComment = comment.copy(remoteParentCommentId = parentId)
                    CommentsApiPayload(CommentError(GENERIC_ERROR, ""), newComment)
                }
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error), comment)
            }
        }
    }

    private fun getXMLRPCCommentStatus(status: CommentStatus): String {
        return when (status) {
            APPROVED -> "approve"
            UNAPPROVED -> "hold"
            SPAM -> "spam"
            TRASH -> "trash"
            else -> "approve"
        }
    }

    private fun getCommentStatusFromXMLRPCStatusString(stringStatus: String): CommentStatus {
        return when (stringStatus) {
            "approve" -> APPROVED
            "hold" -> UNAPPROVED
            "spam" -> SPAM
            "trash" -> TRASH
            else -> APPROVED
        }
    }

    // TODOD: move to mapper?
    private fun commentsResponseToCommentList(response: Any?, site: SiteModel): List<CommentEntity> {
        val comments: MutableList<CommentEntity> = ArrayList()
        if (response !is Array<*>) {
            return comments
        }

        response.forEach { commentObject ->
            commentResponseToComment(commentObject, site)?.let {
                comments.add(it)
            }
        }

        return comments
    }

    // TODOD: move to mapper?
    private fun commentResponseToComment(commentObject: Any?, site: SiteModel): CommentEntity? {
        if (commentObject !is HashMap<*, *>) {
            return null
        }
        val commentMap: HashMap<*, *> = commentObject

        val datePublished = DateTimeUtils.iso8601UTCFromDate(XMLRPCUtils.safeGetMapValue(commentMap, "date_created_gmt", Date()))
        // TODOD: use a wrapper for XMLRPCUtils?
        val remoteParentCommentId = XMLRPCUtils.safeGetMapValue(commentMap, "parent", 0L)

        return CommentEntity(
                //id = 0,
                remoteCommentId = XMLRPCUtils.safeGetMapValue(commentMap, "comment_id", 0L),
                remotePostId = XMLRPCUtils.safeGetMapValue(commentMap, "post_id", 0L),
                remoteParentCommentId = remoteParentCommentId,
                localSiteId = site.id,
                remoteSiteId = site.selfHostedSiteId,
                authorUrl = XMLRPCUtils.safeGetMapValue(commentMap, "author_url", ""),
                authorName = StringEscapeUtils.unescapeHtml4(XMLRPCUtils.safeGetMapValue(commentMap, "author", "")),
                authorEmail = XMLRPCUtils.safeGetMapValue(commentMap, "author_email", ""),
                // TODO: set authorProfileImageUrl - get the hash from the email address?
                // TODOD: check if legacy code place a null in db or empty string!
                authorProfileImageUrl = null,
                postTitle = StringEscapeUtils.unescapeHtml4(
                        XMLRPCUtils.safeGetMapValue(
                                commentMap,
                                "post_title", ""
                        )
                ),
                status = getCommentStatusFromXMLRPCStatusString(XMLRPCUtils.safeGetMapValue(commentMap, "status", "approve")).toString(),
                datePublished = datePublished,
                publishedTimestamp = DateTimeUtils.timestampFromIso8601(datePublished),
                content = XMLRPCUtils.safeGetMapValue(commentMap, "content", ""),
                url = XMLRPCUtils.safeGetMapValue(commentMap, "link", ""),
                hasParent = remoteParentCommentId > 0,
                parentId = if (remoteParentCommentId > 0) remoteParentCommentId else 0,
                iLike = false
        )
    }
}
