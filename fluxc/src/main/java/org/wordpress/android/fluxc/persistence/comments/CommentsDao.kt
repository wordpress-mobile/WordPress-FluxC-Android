package org.wordpress.android.fluxc.persistence.comments

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity

typealias CommentEntityList = List<CommentEntity>

@Dao
abstract class CommentsDao {
    @Transaction
    open suspend fun insertOrUpdateComments(comments: CommentEntityList): List<Long> {
        return comments.map { comment ->
            insertOrUpdateCommentInternal(comment)
        }
    }

    @Transaction
    open suspend fun insertOrUpdateComment(comment: CommentEntity): Long {
        return insertOrUpdateCommentInternal(comment)
    }

    @Query("SELECT * FROM Comments WHERE remoteSiteId = :siteId AND status IN (:statuses) ORDER BY datePublished DESC")
    abstract fun getFilteredComments(siteId: Long, statuses: List<String>): CommentEntityList

    @Query("DELETE FROM Comments")
    abstract suspend fun clearAll(): Int

    @Query("DELETE FROM Comments WHERE remoteSiteId = :siteId AND status IN (:statuses)")
    abstract suspend fun clearAllBySiteId(siteId: Long, statuses: List<String>): Int

    @Query("SELECT count(*) FROM Comments WHERE remoteSiteId = :siteId AND status IN (:statuses)")
    abstract suspend fun getCommentsCountForSite(siteId: Long, statuses: List<String>): Int

    @Insert
    abstract suspend fun insertAll(comments: CommentEntityList)

    @Query("SELECT * FROM Comments WHERE remoteSiteId = :siteId AND remoteCommentId = :remoteCommentId")
    abstract fun getCommentBySiteAndRemoteId(siteId: Long, remoteCommentId: Long): CommentEntityList

    @Transaction
    open suspend fun appendOrOverwriteComments(overwrite: Boolean, siteId: Long, statuses: List<String>, comments: CommentEntityList) {
        if (overwrite) {
            clearAllBySiteId(siteId, statuses)
        }

        insertOrUpdateComments(comments)
    }

    private fun insertOrUpdateCommentInternal(comment: CommentEntity): Long {
        val matchingComments = getMatchingComments(comment.id, comment.remoteCommentId, comment.localSiteId)

        return if (matchingComments.isEmpty()) {
            insert(comment)
        } else {
            // We are forcing the id of the matching comment so the update can
            // act on the expected entity
            val matchingComment = matchingComments.first()

            update(comment.copy(id = matchingComment.id))
            matchingComment.id
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insert(comment: CommentEntity): Long

    @Update
    protected abstract fun update(comment: CommentEntity): Int

    @Query("SELECT * FROM Comments WHERE (id = :commentId OR (remoteCommentId = :remoteCommentId AND localSiteId = :localSiteId))")
    protected abstract fun getMatchingComments(commentId: Long, remoteCommentId: Long, localSiteId: Int): CommentEntityList

    @Entity(
            tableName = "Comments"
    )
    data class CommentEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
        val remoteCommentId: Long,
        val remotePostId: Long,
        val remoteParentCommentId: Long,
        val localSiteId: Int,
        val remoteSiteId: Long,
        val authorUrl: String,
        val authorName: String,
        val authorEmail: String,
        val authorProfileImageUrl: String,
        val postTitle: String,
        val status: String,
        val datePublished: String,
        val publishedTimestamp: Long,
        val content: String,
        val url: String,
        val hasParent: Boolean,
        val parentId: Long,
        val iLike: Boolean
    ){
        @Ignore
        var level: Int = 0
    }

    companion object {
        const val EMPTY_ID = -1L
    }
}
