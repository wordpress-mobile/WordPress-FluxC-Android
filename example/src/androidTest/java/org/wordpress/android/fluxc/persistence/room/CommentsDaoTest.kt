package org.wordpress.android.fluxc.persistence.room

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus.TRASH
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase
import org.wordpress.android.fluxc.persistence.comments.CommentEntityList
import org.wordpress.android.fluxc.persistence.comments.CommentsDao
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class CommentsDaoTest {
    private lateinit var commentsDao: CommentsDao
    private lateinit var db: WPAndroidDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(
                context, WPAndroidDatabase::class.java).allowMainThreadQueries().build()
        commentsDao = db.commentsDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun commentIsInsertedWhenNotPresent() {
        runBlocking {
            val comment = getDefaultComment()

            val entityId = commentsDao.insertOrUpdateComment(comment)

            assertThat(entityId).isEqualTo(1)
        }
    }

    @Test
    fun commentIsUpdatedWhenPresent() {
        runBlocking {
            val comment = getDefaultComment()

            var entityId = commentsDao.insertOrUpdateComment(comment)

            assertThat(entityId).isEqualTo(1)

            val updatedComment = comment.copy(
                    id = entityId,
                    authorUrl = "authorUrl",
                    iLike = true
            )

            entityId = commentsDao.insertOrUpdateComment(updatedComment)

            assertThat(entityId).isEqualTo(1)

            assertThat(commentsDao.getCommentById(entityId).firstOrNull()).isEqualTo(updatedComment)
        }
    }

    @Test
    fun commentIsUpdatedWhenMatchedByRemoteCommentId() {
        runBlocking {
            val comment = getDefaultComment()

            var entityId = commentsDao.insertOrUpdateComment(comment)

            assertThat(entityId).isEqualTo(1)

            val updatedComment = comment.copy(
                    id = 20,
                    authorUrl = "authorUrl",
                    iLike = true
            )

            entityId = commentsDao.insertOrUpdateComment(updatedComment)

            assertThat(entityId).isEqualTo(1)

            assertThat(commentsDao.getCommentById(entityId).firstOrNull()).isEqualTo(updatedComment.copy(id = entityId))
        }
    }

    @Test
    fun insertOrUpdateCanReturnUpdatedComment() {
        runBlocking {
            val comment = getDefaultComment()

            val updatedComment = commentsDao.insertOrUpdateCommentForResult(comment).firstOrNull()

            assertThat(updatedComment).isNotNull
            assertThat(updatedComment?.id).isEqualTo(1)

            assertThat(updatedComment).isEqualTo(comment)
        }
    }

    @Test
    fun getFilteredCommentsIsEmptyWhenFiltersDoNotMatch() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(
                    comments = commentList
            )

            val result = commentsDao.getFilteredComments(100, listOf(TRASH).map { it.toString() })

            assertThat(result).isEmpty()
        }
    }

    @Test
    fun getFilteredCommentsIsNotEmptyWhenFiltersMatch() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(
                    comments = commentList
            )

            val result = commentsDao.getFilteredComments(
                    commentList.first().localSiteId,
                    listOf(APPROVED).map { it.toString() }
            )

            assertThat(
                    result.sortedBy { it.remoteCommentId }
            ).isEqualTo(commentList.filter { it.status == APPROVED.toString() })
        }
    }

    @Test
    fun getFilteredCommentsReturnAllCommentsWhenFiltersAreEmpty() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(
                    comments = commentList
            )

            val result = commentsDao.getFilteredComments(commentList.first().localSiteId, listOf())

            assertThat(result.sortedBy { it.remoteCommentId }).isEqualTo(commentList)
        }
    }

    @Test
    fun getCommentsForSiteLimitsResults() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)

            val result = commentsDao.getCommentsByLocalSiteId(
                    localSiteId = commentList.first().localSiteId,
                    statuses = listOf(),
                    limit = commentList.size - 1,
                    orderAscending = false
            )

            assertThat(result.size).isEqualTo(commentList.size - 1)
        }
    }

    @Test
    fun getCommentsForSiteDoesNotLimitsResults() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)

            val result = commentsDao.getCommentsByLocalSiteId(
                    localSiteId = commentList.first().localSiteId,
                    statuses = listOf(),
                    limit = -1,
                    orderAscending = false
            )

            assertThat(result.size).isEqualTo(commentList.size)
        }
    }

    @Test
    fun getCommentsForSiteFilters() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)

            val result = commentsDao.getCommentsByLocalSiteId(
                    localSiteId = commentList.first().localSiteId,
                    statuses = listOf(APPROVED).map { it.toString() },
                    limit = -1,
                    orderAscending = false
            )

            assertThat(
                    result.sortedBy { it.remoteCommentId }
            ).isEqualTo(commentList.filter { it.status == APPROVED.toString() })
        }
    }

    @Test
    fun getCommentsForSiteOrdersAscending() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)

            val result = commentsDao.getCommentsByLocalSiteId(
                    localSiteId = commentList.first().localSiteId,
                    statuses = listOf(),
                    limit = -1,
                    orderAscending = true
            )

            assertThat(result).isEqualTo(commentList)
        }
    }

    @Test
    fun getCommentsForSiteOrdersDescending() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)

            val result = commentsDao.getCommentsByLocalSiteId(
                    localSiteId = commentList.first().localSiteId,
                    statuses = listOf(),
                    limit = -1,
                    orderAscending = false
            )

            assertThat(result).isEqualTo(commentList.reversed())
        }
    }

    @Test
    fun commentIsDeletedByDeleteComment() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)
            val commentToDelete = commentList.last()

            val result = commentsDao.deleteComment(commentToDelete)

            assertThat(result).isEqualTo(1)
            val comments = commentsDao.getFilteredComments(
                    localSiteId = commentToDelete.localSiteId,
                    statuses = listOf()
            )

            assertThat(comments.size).isEqualTo(commentList.size - 1)
            assertThat(comments.filter { it.remoteCommentId == commentToDelete.remoteCommentId }).isEmpty()
        }
    }

    @Test
    fun commentIsDeletedByRemoteCommentId() {
        runBlocking {
            val commentList = getDefaultCommentList()
            val notPresentId = commentList.map { it.id }.maxOrNull()!! * 10
            commentsDao.appendOrUpdateComments(comments = commentList)
            val commentToDelete = commentList.last().copy(id = notPresentId)

            val result = commentsDao.deleteComment(commentToDelete)

            assertThat(result).isEqualTo(1)
            val comments = commentsDao.getFilteredComments(
                    localSiteId = commentToDelete.localSiteId,
                    statuses = listOf()
            )

            assertThat(comments.size).isEqualTo(commentList.size - 1)
            assertThat(comments.filter { it.remoteCommentId == commentToDelete.remoteCommentId }).isEmpty()
        }
    }

    @Test
    fun getCommentByIdReturnsComment() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)
            val commentToFind = commentList[commentList.size / 2]

            val result = commentsDao.getCommentById(commentToFind.id).first()

            assertThat(result).isEqualTo(commentToFind)
        }
    }

    @Test
    fun getCommentByIdReturnsNothingWhenNoMatch() {
        runBlocking {
            val commentList = getDefaultCommentList()
            val notPresentId = commentList.map { it.id }.maxOrNull()!! * 10
            commentsDao.appendOrUpdateComments(comments = commentList)

            val result = commentsDao.getCommentById(notPresentId)
            assertThat(result).isEmpty()
        }
    }

    @Test
    fun getCommentsByLocalSiteAndRemoteCommentIdReturnsComment() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)
            val commentToFind = commentList[commentList.size / 2]

            val result = commentsDao.getCommentsByLocalSiteAndRemoteCommentId(
                    commentToFind.localSiteId,
                    commentToFind.remoteCommentId
            ).first()

            assertThat(result).isEqualTo(commentToFind)
        }
    }

    @Test
    fun getCommentsByLocalSiteAndRemoteCommentIdReturnsNothingWhenNoMatch() {
        runBlocking {
            val commentList = getDefaultCommentList()
            val notPresentId = commentList.map { it.remoteCommentId }.maxOrNull()!! * 10
            commentsDao.appendOrUpdateComments(comments = commentList)

            val result = commentsDao.getCommentsByLocalSiteAndRemoteCommentId(
                    commentList.first().localSiteId,
                    notPresentId
            )
            assertThat(result).isEmpty()
        }
    }

    @Test
    fun getCommentsByLocalSiteIdAndRemoteCommentIdReturnsComment() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)
            val commentToFind = commentList[commentList.size / 2]

            val result = commentsDao.getCommentsByLocalSiteAndRemoteCommentId(
                    commentToFind.localSiteId,
                    commentToFind.remoteCommentId
            ).first()

            assertThat(result).isEqualTo(commentToFind)
        }
    }

    @Test
    fun getCommentsByLocalSiteIdAndRemoteCommentIdReturnsNothingWhenNoMatch() {
        runBlocking {
            val commentList = getDefaultCommentList()
            val notPresentId = commentList.map { it.remoteCommentId }.maxOrNull()!! * 10
            commentsDao.appendOrUpdateComments(comments = commentList)

            val result = commentsDao.getCommentsByLocalSiteAndRemoteCommentId(
                    commentList.first().localSiteId,
                    notPresentId
            )
            assertThat(result).isEmpty()
        }
    }

    @Test
    fun clearAllBySiteIdAndFiltersRemovesBySite() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)

            val result = commentsDao.clearAllBySiteIdAndFilters(
                    commentList.first().localSiteId,
                    listOf()
            )

            assertThat(result).isEqualTo(commentList.size)
        }
    }

    @Test
    fun clearAllBySiteIdAndFiltersRemovesByFilters() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)

            val result = commentsDao.clearAllBySiteIdAndFilters(
                    commentList.first().localSiteId,
                    listOf(SPAM.toString())
            )

            assertThat(result).isEqualTo(commentList.filter { it.status == SPAM.toString() }.size)
        }
    }

    @Test
    fun clearAllBySiteIdAndFiltersCanRemoveNothing() {
        runBlocking {
            val commentList = getDefaultCommentList()
            commentsDao.appendOrUpdateComments(comments = commentList)

            val result = commentsDao.clearAllBySiteIdAndFilters(
                    commentList.first().localSiteId,
                    listOf(TRASH.toString())
            )

            assertThat(result).isEqualTo(0)
        }
    }

    private fun getDefaultComment() = CommentEntity(
            id = 1,
            remoteCommentId = 10,
            remotePostId = 100,
            remoteParentCommentId = 1_000,
            localSiteId = 10_000,
            remoteSiteId = 100_000,
            authorUrl = null,
            authorName = null,
            authorEmail = null,
            authorProfileImageUrl = null,
            postTitle = null,
            status = APPROVED.toString(),
            datePublished = null,
            publishedTimestamp = 1_000_000,
            content = null,
            url = null,
            hasParent = false,
            parentId = 10_000_000,
            iLike = false
    )

    private fun getDefaultCommentList(): CommentEntityList {
        val comment = getDefaultComment()
        return listOf(
                comment.copy(
                        id = 1,
                        remoteCommentId = 10,
                        datePublished = "2021-07-24T00:51:43+02:00",
                        status = APPROVED.toString()
                ),
                comment.copy(
                        id = 2,
                        remoteCommentId = 20,
                        datePublished = "2021-07-24T00:52:43+02:00",
                        status = UNAPPROVED.toString()
                ),
                comment.copy(
                        id = 3,
                        remoteCommentId = 30,
                        datePublished = "2021-07-24T00:53:43+02:00",
                        status = APPROVED.toString()
                ),
                comment.copy(
                        id = 4,
                        remoteCommentId = 40,
                        datePublished = "2021-07-24T00:54:43+02:00",
                        status = SPAM.toString()
                )
        )
    }
}
