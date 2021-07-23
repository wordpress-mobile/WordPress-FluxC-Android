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
    abstract suspend fun getFilteredComments(siteId: Long, statuses: List<String>): CommentEntityList

    @Query("DELETE FROM Comments")
    abstract suspend fun clearAll(): Int

    @Query("DELETE FROM Comments WHERE remoteSiteId = :siteId AND status IN (:statuses)")
    abstract suspend fun clearAllBySiteId(siteId: Long, statuses: List<String>): Int

    @Transaction
    open suspend fun deleteComment(comment: CommentEntity): Int {
        val result = deleteById(comment.id)

        return if (result > 0) {
            result
        } else {
            deleteByRemoteIds(comment.remoteSiteId, comment.remoteCommentId)
        }
    }

    @Query("DELETE FROM Comments WHERE id = :commentId")
    protected abstract fun deleteById(commentId: Long): Int

    @Query("DELETE FROM Comments WHERE remoteSiteId = :siteId OR remoteCommentId = :remoteCommentId")
    protected abstract fun deleteByRemoteIds(siteId: Long, remoteCommentId: Long): Int

    @Query("SELECT count(*) FROM Comments WHERE remoteSiteId = :siteId AND status IN (:statuses)")
    abstract suspend fun getCommentsCountForSite(siteId: Long, statuses: List<String>): Int

    //@Insert
    //abstract suspend fun insertAll(comments: CommentEntityList)


    //@Query("SELECT * FROM Comments WHERE id = :localId")
    //abstract suspend fun getCommentsById(localId: Long): CommentEntityList

    @Query("SELECT * FROM Comments WHERE id = :localId LIMIT 1")
    abstract suspend fun getCommentById(localId: Long): CommentEntityList

    @Deprecated("This has been introduced for legacy compatibility until full migration to room (see CommentsStoreAdapter in WPAndroid)")
    @Query("SELECT * FROM Comments WHERE localSiteId = :localSiteId AND remoteCommentId = :remoteCommentId")
    abstract suspend fun getCommentsByLocalSiteAndRemoteCommentId(localSiteId: Int, remoteCommentId: Long): CommentEntityList


    @Query("SELECT * FROM Comments WHERE remoteSiteId = :siteId AND remoteCommentId = :remoteCommentId")
    abstract fun getCommentsBySiteIdAndRemoteCommentId(siteId: Long, remoteCommentId: Long): CommentEntityList

    @Transaction
    open suspend fun appendOrOverwriteComments(overwrite: Boolean, siteId: Long, statuses: List<String>, comments: CommentEntityList): Int {
        if (overwrite) {
            clearAllBySiteId(siteId, statuses)
        }

        val affectedIdList = insertOrUpdateComments(comments)
        return affectedIdList.size
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
        val authorUrl: String?,
        val authorName: String?,
        val authorEmail: String?,
        val authorProfileImageUrl: String?,
        val postTitle: String?,
        val status: String?,
        val datePublished: String?,
        val publishedTimestamp: Long,
        val content: String?,
        val url: String?,
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
