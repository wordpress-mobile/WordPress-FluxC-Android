package org.wordpress.android.fluxc.persistence.comments

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class CommentsDao {
    @Transaction
    open suspend fun insertOrUpdateComments(comments: List<CommentEntity>): List<Long> {
        return comments.map { comment ->
            insertOrUpdateCommentInternal(comment)
        }
    }

    @Transaction
    open suspend fun insertOrUpdateComment(comment: CommentEntity): Long {
        return insertOrUpdateCommentInternal(comment)
    }

    @Query("SELECT * FROM Comments WHERE remoteSiteId = :siteId AND status IN (:statuses) ORDER BY datePublished DESC")
    abstract fun pagingSource(siteId: Long, statuses: List<String>): PagingSource<Int, CommentEntity>

    @Query("DELETE FROM Comments")
    abstract suspend fun clearAll(): Int

    @Query("DELETE FROM Comments WHERE remoteSiteId = :siteId AND status IN (:statuses)")
    abstract suspend fun clearAllBySiteId(siteId: Long, statuses: List<String>): Int

    @Query("SELECT count(*) FROM Comments WHERE remoteSiteId = :siteId AND status IN (:statuses)")
    abstract suspend fun getCommentsCountForSite(siteId: Long, statuses: List<String>): Int

    @Insert
    abstract suspend fun insertAll(comments: List<CommentEntity>)

    @Query("SELECT * FROM Comments WHERE remoteSiteId = :siteId AND remoteCommentId = :remoteCommentId")
    abstract fun getCommentBySiteAndRemoteId(siteId: Long, remoteCommentId: Long): List<CommentEntity>

    @Transaction
    open suspend fun appendOrOverwriteComments(overwrite: Boolean, siteId: Long, statuses: List<String>, comments: List<CommentEntity>) {
        if (overwrite) {
            clearAllBySiteId(siteId, statuses)
            //clearAll()
        }

        insertOrUpdateComments(comments)
        //insertAll(comments)

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

    //@Query("SELECT * FROM Comments WHERE id = :id LIMIT 1")
    //protected abstract fun getCommentById(id: Long): List<CommentEntity>

    @Query("SELECT * FROM Comments WHERE (id = :commentId OR (remoteCommentId = :remoteCommentId AND localSiteId = :localSiteId))")
    protected abstract fun getMatchingComments(commentId: Long, remoteCommentId: Long, localSiteId: Int): List<CommentEntity>

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
    )

    companion object {
        const val EMPTY_ID = -1L
    }
}
