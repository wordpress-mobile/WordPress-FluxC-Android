package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.CommentAction
import org.wordpress.android.fluxc.action.CommentsAction
import org.wordpress.android.fluxc.action.CommentsAction.CREATE_NEW_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.DELETE_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.FETCH_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.FETCH_COMMENTS
import org.wordpress.android.fluxc.action.CommentsAction.LIKE_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.PUSH_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.REMOVE_ALL_COMMENTS
import org.wordpress.android.fluxc.action.CommentsAction.REMOVE_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.REMOVE_COMMENTS
import org.wordpress.android.fluxc.action.CommentsAction.UPDATE_COMMENT
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentsRestClient
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentXMLRPCClient
import org.wordpress.android.fluxc.persistence.comments.CommentEntityList
import org.wordpress.android.fluxc.persistence.comments.CommentsDao
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.DUPLICATE_COMMENT
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.INVALID_INPUT
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.UNKNOWN_COMMENT
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload
import org.wordpress.android.fluxc.store.CommentStore.RemoteLikeCommentPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionData
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionEntityInfo
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.PagingData
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentsStore
@Inject constructor(
    private val commentsRestClient: CommentsRestClient,
    private val commentXMLRPCClient: CommentXMLRPCClient,
    private val commentsDao: CommentsDao,
    private val commentsMapper: CommentsMapper,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    data class CommentsActionPayload<T>(
        val data: T? = null
    ) : Payload<CommentError>() {
        constructor(error: CommentError) : this() {
            this.error = error
        }

        constructor(error: CommentError, data: T?) : this(data) {
            this.error = error
        }
    }

    sealed class CommentsData {
        data class PagingData(val comments: CommentEntityList, val hasMore: Boolean): CommentsData() {
            companion object {
                fun empty() = PagingData(comments = listOf(), hasMore = false)
            }
        }
        data class CommentsActionData(val comments: CommentEntityList, val rowsAffected: Int): CommentsData()
        data class CommentsActionEntityInfo(val entityIds: List<Long>, val rowsAffected: Int): CommentsData()
        object DontCare: CommentsData()
        //data class ModeratedCommentInfo(val commentId: Long): CommentsData()
    }

    suspend fun fetchComments(
        site: SiteModel,
        number: Int,
        offset: Int,
        networkStatusFilter: CommentStatus,
        cacheStatuses: List<CommentStatus>
    ): CommentsActionPayload<PagingData> {
        // TODOD: manage for self-hosted!
        val payload = commentsRestClient.fetchComments(
                site = site,
                number = number,
                offset = offset,
                status = networkStatusFilter
        )

        return if (payload.isError) {
            val cachedComments = commentsDao.getFilteredComments(
                    siteId = site.siteId,
                    statuses = cacheStatuses.map { it.toString() }
            )
            CommentsActionPayload(payload.error, PagingData(comments = cachedComments, hasMore = true))
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

            val cachedComments = commentsDao.getFilteredComments(
                    siteId = site.siteId,
                    statuses = cacheStatuses.map { it.toString() }
            )

            CommentsActionPayload(PagingData(comments = cachedComments, hasMore = comments.size == number))
        }
    }

    suspend fun moderateCommentLocally(
        site: SiteModel,
        remoteCommentId: Long,
        newStatus: CommentStatus
    ): CommentsActionPayload<CommentsActionData> {
        val comments = commentsDao.getCommentsBySiteIdAndRemoteCommentId(site.siteId, remoteCommentId)

        if (comments.isEmpty()) {
            return CommentsActionPayload(
                    CommentError(
                            UNKNOWN_COMMENT,
                            "Unknown comment while moderating [site=${site.siteId} remoteCommentId=$remoteCommentId]"
                    )
            )
        }

        if (comments.size > 1) {
            return CommentsActionPayload(
                    CommentError(
                            DUPLICATE_COMMENT,
                            "Duplicated comment while moderating [site=${site.siteId} remoteCommentId=$remoteCommentId]"
                    )
            )
        }

        val comment = comments.first().copy(status = newStatus.toString())
        val entityId = commentsDao.insertOrUpdateComment(comment)
        val cachedCommentList = commentsDao.getCommentById(entityId)

        return CommentsActionPayload(CommentsActionData(
                comments = cachedCommentList,
                rowsAffected = cachedCommentList.size
        ))
    }

    suspend fun getCommentByLocalId(localId: Long) = commentsDao.getCommentById(localId)

    suspend fun getCommentBySiteAndRemoteId(localSiteId: Int, remoteCommentId: Long) =
            commentsDao.getCommentsByLocalSiteAndRemoteCommentId(localSiteId, remoteCommentId)

    @Deprecated("Action and event bus support should be gradually replaced while the Comments Unification project proceeds")
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? CommentsAction ?: return

        when (actionType) {
            FETCH_COMMENTS -> {
                coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On FETCH_COMMENTS") {
                    emitChange(TODO())
                }
            }
            FETCH_COMMENT -> {
                coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On FETCH_COMMENT") {
                    emitChange(onFetchComment(action.payload as RemoteCommentPayload))
                }
            }
            CREATE_NEW_COMMENT -> {
                coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On CREATE_NEW_COMMENT") {
                    emitChange(onCreateNewComment(action.payload as RemoteCreateCommentPayload))
                }
            }
            PUSH_COMMENT -> {
                coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On PUSH_COMMENT") {
                    emitChange(onPushComment(action.payload as RemoteCommentPayload))
                }
            }
            DELETE_COMMENT -> {
                coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On DELETE_COMMENT") {
                    emitChange(onDeleteComment(action.payload as RemoteCommentPayload))
                }
            }
            LIKE_COMMENT -> {
                coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On LIKE_COMMENT") {
                    emitChange(onLikeComment(action.payload as RemoteLikeCommentPayload))
                }
            }
            //FETCHED_COMMENTS -> {
            //    coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On FETCHED_COMMENTS") {
            //        emitChange(TODO())
            //    }
            //}
            //FETCHED_COMMENT -> {
            //    coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On FETCHED_COMMENT") {
            //        emitChange(TODO())
            //    }
            //}
            //CREATED_NEW_COMMENT -> {
            //    coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On CREATED_NEW_COMMENT") {
            //        emitChange(TODO())
            //    }
            //}
            //PUSHED_COMMENT -> {
            //    coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On PUSHED_COMMENT") {
            //        emitChange(TODO())
            //    }
            //}
            //DELETED_COMMENT -> {
            //    coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On DELETED_COMMENT") {
            //        emitChange(TODO())
            //    }
            //}
            //LIKED_COMMENT -> {
            //    coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On LIKED_COMMENT") {
            //        emitChange(TODO())
            //    }
            //}
            UPDATE_COMMENT -> {
                coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On UPDATE_COMMENT") {
                    emitChange(onUpdateComment(action.payload as CommentModel))
                }
            }
            REMOVE_COMMENTS -> {
                coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On REMOVE_COMMENTS") {
                    emitChange(TODO())
                }
            }
            REMOVE_COMMENT -> {
                coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On REMOVE_COMMENT") {
                    emitChange(TODO())
                }
            }
            REMOVE_ALL_COMMENTS -> {
                coroutineEngine.launch(AppLog.T.API, this, "CommentsStore: On REMOVE_ALL_COMMENTS") {
                    emitChange(TODO())
                }
            }
        }
    }

    private suspend fun onFetchComment(payload: RemoteCommentPayload): OnCommentChanged {
        val commentId = payload.comment?.remoteCommentId ?: payload.remoteCommentId
        val response = fetchComment(payload.site, commentId)

        return if (response.isError) {
            OnCommentChanged(0).apply {
                this.causeOfChange = CommentAction.FETCH_COMMENT
                this.error = response.error
            }
        } else {
            val rowsAffected = response.data?.rowsAffected.orNone()
            OnCommentChanged(rowsAffected).apply {
                response.data?.let { commentsData ->
                    commentsData.comments.firstOrNull()?.id?.let {
                        this.changedCommentsLocalIds.add(it.toInt())
                    }
                }

                this.causeOfChange = CommentAction.FETCH_COMMENT
            }
        }
    }

    private suspend fun onUpdateComment(payload: CommentModel): OnCommentChanged {
        val response = updateComment(
                isError = payload.isError,
                commentId = payload.id.toLong(),
                comment = commentsMapper.commentLegacyModelToEntity(payload)
        )

        return OnCommentChanged(response.data?.rowsAffected.orNone()).apply {
            this.changedCommentsLocalIds.addAll(response.data?.entityIds?.map { it.toInt() } ?: listOf())
            this.causeOfChange = CommentAction.UPDATE_COMMENT
        }
    }

    private suspend fun onDeleteComment(payload: RemoteCommentPayload): OnCommentChanged {
        //val commentId = payload.comment?.remoteCommentId ?: payload.remoteCommentId
        val response = deleteComment(payload.site, payload.remoteCommentId, payload.comment?.let { commentsMapper.commentLegacyModelToEntity(it) })

        // TODOD: keeping here the rowsAffected set to 0 as it is in original handleDeletedCommentResponse; better get the rationale!
        return OnCommentChanged(0).apply {
            this.changedCommentsLocalIds.addAll(response.data.toCommentIdsListOrEmpty())
            this.causeOfChange = CommentAction.DELETE_COMMENT
            this.error = response.error
        }
    }

    private suspend fun onLikeComment(payload: RemoteLikeCommentPayload): OnCommentChanged {
        val response = likeComment(
                payload.site,
                payload.remoteCommentId,
                payload.comment?.let { commentsMapper.commentLegacyModelToEntity(it) },
                payload.like
        )

        return OnCommentChanged(response.data?.rowsAffected.orNone()).apply {
            this.changedCommentsLocalIds.addAll(response.data.toCommentIdsListOrEmpty())
            this.causeOfChange = CommentAction.LIKE_COMMENT
            this.error = response.error
        }
    }

    private suspend fun onPushComment(payload: RemoteCommentPayload): OnCommentChanged {
        if (payload.comment == null) {
            return OnCommentChanged(0).apply {
                this.causeOfChange = CommentAction.PUSH_COMMENT
                this.error = CommentError(INVALID_INPUT, "Comment can't be null")
            }
        }

        val response = pushComment(
                payload.site,
                commentsMapper.commentLegacyModelToEntity(payload.comment)
        )

        return OnCommentChanged(response.data?.rowsAffected.orNone()).apply {
            this.changedCommentsLocalIds.addAll(response.data.toCommentIdsListOrEmpty())
            this.causeOfChange = CommentAction.PUSH_COMMENT
            this.error = response.error
        }
    }

    private suspend fun onCreateNewComment(payload: RemoteCreateCommentPayload): OnCommentChanged {
        val response = if (payload.reply == null) {
            // Create a new comment on a specific Post
            createNewComment(
                    payload.site,
                    commentsMapper.commentLegacyModelToEntity(payload.comment)
            )
        } else {
            // Create a new reply to a specific Comment
            createNewReplay(payload.site, commentsMapper.commentLegacyModelToEntity(payload.comment), commentsMapper.commentLegacyModelToEntity(payload.reply))
        }

        return OnCommentChanged(response.data?.rowsAffected.orNone()).apply {
            this.changedCommentsLocalIds.addAll(response.data.toCommentIdsListOrEmpty())
            this.causeOfChange = CommentAction.CREATE_NEW_COMMENT
            this.error = response.error
        }
    }

    private suspend fun createNewComment(site: SiteModel, comment: CommentEntity): CommentsActionPayload<CommentsActionData> {
        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.createNewComment(site, comment.remotePostId, comment.content)
        } else {
            // TODOD: implement push for self-hosted
            TODO()
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error, CommentsActionData(comment.toListOrEmpty(), 0))
        } else {
            payload.response?.let {
                val commentUpdated = commentsMapper.commentDtoToEntity(it, site).copy(id = comment.id)
                val entityId = commentsDao.insertOrUpdateComment(commentUpdated)
                // We need to get it back from the cache in case it was inserted instead of updated
                val cachedCommentList = commentsDao.getCommentById(entityId)
                CommentsActionPayload(CommentsActionData(cachedCommentList, 1))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, ""))
        }
    }

    private suspend fun createNewReplay(site: SiteModel, comment: CommentEntity, reply: CommentEntity): CommentsActionPayload<CommentsActionData> {
        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.createNewReply(site, comment.remoteCommentId, reply.content)
        } else {
            // TODOD: implement push for self-hosted
            TODO()
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error, CommentsActionData(reply.toListOrEmpty(), 0))
        } else {
            payload.response?.let {
                val commentUpdated = commentsMapper.commentDtoToEntity(it, site).copy(id = reply.id)
                val entityId = commentsDao.insertOrUpdateComment(commentUpdated)
                // We need to get it back from the cache in case it was inserted instead of updated
                val cachedCommentList = commentsDao.getCommentById(entityId)
                CommentsActionPayload(CommentsActionData(cachedCommentList, 1))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, ""))
        }

        // TODOD: apply the following logic
        //CommentModel newComment = commentResponseToComment(response, site);
        //newComment.setId(reply.getId()); // reconciliate local instance and newly created object
        //RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(newComment);
    }


    suspend fun pushLocalCommentByRemoteId(site: SiteModel, remoteCommentId: Long): CommentsActionPayload<CommentsActionData> {
        val comment = commentsDao.getCommentsBySiteIdAndRemoteCommentId(site.siteId, remoteCommentId).firstOrNull()
                ?: return CommentsActionPayload(CommentError(INVALID_INPUT, ""))

        return pushComment(site, comment)
    }



    suspend fun pushComment(site: SiteModel, comment: CommentEntity): CommentsActionPayload<CommentsActionData> {
        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.pushComment(site, comment)
        } else {
            // TODOD: implement push for self-hosted
            TODO()
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error, CommentsActionData(comment.toListOrEmpty(), 0))
        } else {
            payload.response?.let {
                val commentUpdated = commentsMapper.commentDtoToEntity(it, site).copy(id = comment.id)
                val entityId = commentsDao.insertOrUpdateComment(commentUpdated)
                // We need to get it back from the cache in case it was inserted instead of updated
                val cachedCommentList = commentsDao.getCommentById(entityId)
                CommentsActionPayload(CommentsActionData(cachedCommentList, 1))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, ""))
        }
    }


    private suspend fun likeComment(site: SiteModel, remoteCommentId: Long, comment: CommentEntity?, isLike: Boolean): CommentsActionPayload<CommentsActionData> {
        // If the comment is stored locally, we want to update it locally (needed because in some
        // cases we use this to update comments by remote id).
        val commentToLike = comment ?: commentsDao.getCommentsByLocalSiteAndRemoteCommentId(site.id, remoteCommentId).firstOrNull()
        val remoteCommentIdToLike = commentToLike?.remoteCommentId ?: remoteCommentId

        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.likeComment(site, remoteCommentIdToLike, isLike)
        } else {
            TODO()
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error, CommentsActionData(commentToLike.toListOrEmpty(), 0))
        } else {
            payload.response?.let { endpointResponse ->
                val updatedComment = commentToLike?.let { it.copy(iLike = endpointResponse.i_like) }

                val rowsAffected = updatedComment?.let {
                    commentsDao.insertOrUpdateComment(it)
                    1
                } ?: 0

                CommentsActionPayload(CommentsActionData(updatedComment.toListOrEmpty(), rowsAffected))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, ""))
        }
    }

    private suspend fun deleteComment(site: SiteModel, remoteCommentId: Long, comment: CommentEntity?): CommentsActionPayload<CommentsActionData> {
        // If the comment is stored locally, we want to update it locally (needed because in some
        // cases we use this to update comments by remote id).
        val commentToDelete = comment ?: commentsDao.getCommentsByLocalSiteAndRemoteCommentId(site.id, remoteCommentId).firstOrNull()
        val remoteCommentIdToDelete = commentToDelete?.remoteCommentId ?: remoteCommentId

        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.deleteComment(site, remoteCommentIdToDelete)
        } else {
            TODO()
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error, CommentsActionData(commentToDelete.toListOrEmpty(), 0))
        } else {
            payload.response?.let {
                val commentFromEndpoint = commentsMapper.commentDtoToEntity(it, site)
                val comment = commentToDelete?.let { comment ->
                    commentFromEndpoint.copy(id = comment.id)
                } ?: commentFromEndpoint

                if (comment.status?.equals(CommentStatus.DELETED.toString()) == true) {
                    commentsDao.deleteComment(comment)
                } else {
                    commentsDao.insertOrUpdateComment(comment)
                }

                CommentsActionPayload(CommentsActionData(comment.toListOrEmpty(), 1))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, ""))
        }
    }

    private suspend fun updateComment(isError: Boolean, commentId: Long, comment: CommentEntity): CommentsActionPayload<CommentsActionEntityInfo> {
        val (entityId, rowsAffected) = if (isError) {
            Pair(commentId, 0)
        } else {
            Pair(commentsDao.insertOrUpdateComment(comment), 1)
        }

        return CommentsActionPayload(CommentsActionEntityInfo(listOf(entityId), rowsAffected))
    }

    private suspend fun fetchComment(site: SiteModel, remoteCommentId: Long): CommentsActionPayload<CommentsActionData> {
        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.fetchComment(site, remoteCommentId)
        } else {
            TODO()
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error)
        } else {
            payload.response?.let {
                val comment = commentsMapper.commentDtoToEntity(it, site)
                val entityId = commentsDao.insertOrUpdateComment(comment)
                //val rowsAffected = if (entityId > 0) 1 else 0
                val cachedCommentList = commentsDao.getCommentById(entityId)
                CommentsActionPayload(CommentsActionData(cachedCommentList, 1))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, ""))
        }
    }

    // TODOD: review implementation to align with other functions eventually
    suspend fun getCachedComments(
        site: SiteModel,
        cacheStatuses: List<CommentStatus>,
        imposeHasMore: Boolean
    ): CommentsActionPayload<PagingData> {
        val cachedComments = commentsDao.getFilteredComments(
                siteId = site.siteId,
                statuses = cacheStatuses.map { it.toString() }
        )

        return CommentsActionPayload(PagingData(comments = cachedComments, imposeHasMore))
    }


    @Deprecated("Action and event bus support should be gradually replaced while the Comments Unification project proceeds")
    override fun onRegister() {
        AppLog.d(AppLog.T.API, this.javaClass.name + ": onRegister")
    }

    //private fun CommentEntityList.sublistToLimitOrEmpty(limit: Int): CommentEntityList {
    //    require(limit >= 0) {"sublistOrEmpty cannot accept negative values [$limit] for limit parameter"}
//
    //    return if (this.size <= limit) {
    //        this
    //    } else {
    //        this.subList(0, limit)
    //    }
    //}

    private fun CommentsActionData?.toCommentIdsListOrEmpty(): List<Int> {
        return this?.comments?.map { it.id.toInt() } ?: listOf()
    }

    private fun CommentEntity?.toListOrEmpty(): List<CommentEntity> {
        return this?.let {
            listOf(it)
        } ?: listOf()
    }

    private fun Int?.orNone(): Int {
        return this ?: 0
    }
}
