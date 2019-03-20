package org.wordpress.android.fluxc.release

import android.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.list.AuthorFilter
import org.wordpress.android.fluxc.model.list.ListOrder
import org.wordpress.android.fluxc.model.list.ListOrder.DESC
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListOrderBy
import org.wordpress.android.fluxc.model.list.PostListOrderBy.DATE
import org.wordpress.android.fluxc.model.list.datastore.PostListDataStore
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.model.post.PostStatus.TRASHED
import org.wordpress.android.fluxc.release.AuthorTestFilter.EVERYONE
import org.wordpress.android.fluxc.release.AuthorTestFilter.SPECIFIC_AUTHOR
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestHelper
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode.MultiplePages
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode.SinglePage
import org.wordpress.android.fluxc.release.utils.TEST_LIST_CONFIG
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.DEFAULT_POST_STATUS_LIST
import javax.inject.Inject

private const val TEST_POST_LIST_SEARCH_QUERY = "a"

enum class AuthorTestFilter { EVERYONE, SPECIFIC_AUTHOR }

internal class RestPostListTestCase(
    val statusList: List<PostStatus> = DEFAULT_POST_STATUS_LIST,
    val author: AuthorTestFilter = EVERYONE,
    val order: ListOrder = DESC,
    val orderBy: PostListOrderBy = DATE,
    val searchQuery: String? = null,
    val testMode: ListStoreConnectedTestMode = SinglePage(false)
)

@RunWith(Parameterized::class)
internal class ReleaseStack_PostListTestWpCom(
    private val testCase: RestPostListTestCase
) : ReleaseStack_WPComBase() {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject internal lateinit var listStore: ListStore
    @Inject internal lateinit var postStore: PostStore

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): List<RestPostListTestCase> = listOf(
                /*
                 These test cases are specifically picked to be non-demanding so that it can run without much setup.
                 They are very easy to extend on, so if we start running them on an account with pre-setup, they can
                 made to be a lot more demanding by ensuring that each post list type returns some data.
                 */
                RestPostListTestCase(testMode = MultiplePages),
                RestPostListTestCase(statusList = listOf(DRAFT)),
                RestPostListTestCase(statusList = listOf(SCHEDULED)),
                RestPostListTestCase(statusList = listOf(TRASHED)),
                RestPostListTestCase(order = ListOrder.ASC, testMode = MultiplePages),
                RestPostListTestCase(orderBy = PostListOrderBy.ID, testMode = MultiplePages),
                RestPostListTestCase(searchQuery = TEST_POST_LIST_SEARCH_QUERY),
                RestPostListTestCase(
                        author = SPECIFIC_AUTHOR,
                        testMode = SinglePage(ensureListIsNotEmpty = true)
                )
        )
    }

    private val listStoreConnectedTestHelper by lazy {
        ListStoreConnectedTestHelper(listStore)
    }

    private val postListDataStore by lazy {
        PostListDataStore(mDispatcher, postStore, sSite)
    }

    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init(testCase.author == SPECIFIC_AUTHOR)
    }

    @Test
    fun test() {
        listStoreConnectedTestHelper.runTest(testCase.testMode, this::createPagedListWrapper)
    }

    private fun createPagedListWrapper(): PagedListWrapper<PostModel> {
        val authorFilter: AuthorFilter = when (testCase.author) {
            EVERYONE -> AuthorFilter.SpecificAuthor(mAccountStore.account.userId)
            SPECIFIC_AUTHOR -> AuthorFilter.Everyone
        }

        val descriptor = PostListDescriptorForRestSite(
                site = sSite,
                statusList = testCase.statusList,
                author = authorFilter,
                order = testCase.order,
                orderBy = testCase.orderBy,
                searchQuery = testCase.searchQuery,
                config = TEST_LIST_CONFIG
        )
        return listStoreConnectedTestHelper.getList(descriptor, postListDataStore)
    }
}
