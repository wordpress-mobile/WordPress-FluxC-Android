package org.wordpress.android.fluxc.network.rest.wpcom.comment

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
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
import org.wordpress.android.fluxc.store.comments.CommentsStore.CommentsActionPayload
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
    suspend fun fetchComments(
        site: SiteModel,
        number: Int,
        offset: Int,
        status: CommentStatus
    ): CommentsActionPayload<CommentsWPComRestResponse> {
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
                CommentsActionPayload(response.data)
            }
            is Error -> {
                CommentsActionPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }


    suspend fun pushComment(site: SiteModel, comment: CommentEntity): CommentsActionPayload<CommentWPComRestResponse> {
        val url = WPCOMREST.sites.site(site.siteId).comments.comment(comment.remoteCommentId).urlV1_1

        val request = mutableMapOf(
                "content" to comment.content,
                "date" to comment.datePublished,
                "status" to comment.status
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
                CommentsActionPayload(response.data)
            }
            is Error -> {
                CommentsActionPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }


}
