package org.wordpress.android.fluxc.network.rest.wpcom.comment

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentWPComRestResponse.CommentsWPComRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.common.LikesUtilsProvider
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.utils.CommentErrorUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CommentsRestClient @Inject constructor(
    appContext: Context?,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    private val likesUtilsProvider: LikesUtilsProvider,
    private val commentErrorUtilsWrapper: CommentErrorUtilsWrapper
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    data class FetchCommentsPayload<T>(
        val response: T? = null
    ) : Payload<CommentError>() {
        constructor(error: CommentError) : this() {
            this.error = error
        }
    }

    suspend fun fetchComments(
        site: SiteModel,
        number: Int,
        offset: Int,
        status: CommentStatus
    ): FetchCommentsPayload<CommentsWPComRestResponse> {
        val url = WPCOMREST.sites.site(site.siteId).comments.urlV1_1

        val params = mutableMapOf(
                "status" to status.toString(),
                "offset" to offset.toString(),
                "number" to number.toString(),
                "force" to "wpcom"
        )

        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                CommentsWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                FetchCommentsPayload(response.data)
            }
            is Error -> {
                FetchCommentsPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }


    suspend fun pushComment(site: SiteModel, comment: CommentEntity): FetchCommentsPayload<CommentWPComRestResponse> {
        val url = WPCOMREST.sites.site(site.siteId).comments.comment(comment.remoteCommentId).urlV1_1

        val request = mutableMapOf(
                "content" to comment.content.orEmpty(),
                "date" to comment.datePublished.orEmpty(),
                "status" to comment.status.orEmpty()
        )

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                request,
                CommentWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                FetchCommentsPayload(response.data)
            }
            is Error -> {
                FetchCommentsPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun fetchComment(site: SiteModel, remoteCommentId: Long): FetchCommentsPayload<CommentWPComRestResponse> {
        val url = WPCOMREST.sites.site(site.siteId).comments.comment(remoteCommentId).urlV1_1

        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                mapOf(),
                CommentWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                FetchCommentsPayload(response.data)
            }
            is Error -> {
                FetchCommentsPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun deleteComment(site: SiteModel, remoteCommentId: Long): FetchCommentsPayload<CommentWPComRestResponse> {
        val url = WPCOMREST.sites.site(site.siteId).comments.comment(remoteCommentId).delete.urlV1_1

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                null,
                CommentWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                FetchCommentsPayload(response.data)
            }
            is Error -> {
                FetchCommentsPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun likeComment(site: SiteModel, remoteCommentId: Long/*, comment: CommentEntity*/, isLike: Boolean): FetchCommentsPayload<CommentLikeWPComRestResponse> {
        val url = if (isLike) {
            WPCOMREST.sites.site(site.siteId).comments.comment(remoteCommentId).likes.new_.urlV1_1
        } else {
            WPCOMREST.sites.site(site.siteId).comments.comment(remoteCommentId).likes.mine.delete.urlV1_1
        }

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                null,
                CommentLikeWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                FetchCommentsPayload(response.data)
            }
            is Error -> {
                FetchCommentsPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun createNewComment(site: SiteModel, remotePostId: Long, content: String?): FetchCommentsPayload<CommentWPComRestResponse> {
        val url = WPCOMREST.sites.site(site.siteId).posts.post(remotePostId).replies.new_.urlV1_1

        val request = mutableMapOf(
                "content" to content.orEmpty(),
        )

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                request,
                CommentWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                FetchCommentsPayload(response.data)
            }
            is Error -> {
                FetchCommentsPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun createNewReply(site: SiteModel, remoteCommentId: Long, replayContent: String?): FetchCommentsPayload<CommentWPComRestResponse> {
        val url = WPCOMREST.sites.site(site.siteId).comments.comment(remoteCommentId).replies.new_.urlV1_1

        val request = mutableMapOf(
                "content" to replayContent.orEmpty(),
        )

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                request,
                CommentWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                FetchCommentsPayload(response.data)
            }
            is Error -> {
                FetchCommentsPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }




}
