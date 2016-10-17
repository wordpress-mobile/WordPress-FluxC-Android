package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.InstantiatePostPayload;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostInstantiated;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.utils.WellSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_PostTestWPCOM extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject PostStore mPostStore;
    @Inject SiteStore mSiteStore;

    private static final String POST_DEFAULT_TITLE = "PostTestWPCOM base post";
    private static final String POST_DEFAULT_DESCRIPTION = "Hi there, I'm a post from FluxC!";

    private CountDownLatch mCountDownLatch;
    private PostModel mPost;
    private static SiteModel mSite;

    private boolean mCanLoadMorePosts;

    private enum TEST_EVENTS {
        NONE,
        SITE_CHANGED,
        POST_INSTANTIATED,
        POST_UPDATED,
        POSTS_FETCHED,
        PAGES_FETCHED,
        POST_DELETED,
        ERROR_UNKNOWN_POST,
        ERROR_UNKNOWN_POST_TYPE,
        ERROR_GENERIC
    }
    private TEST_EVENTS mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
        // Reset expected test event
        mNextEvent = TEST_EVENTS.NONE;

        mPost = null;
        mCanLoadMorePosts = false;

        if (mAccountStore.getAccessToken().isEmpty()) {
            authenticate();
        }

        if (mSite == null) {
            fetchSites();
            mSite = mSiteStore.getSites().get(0);
        }
    }

    public void testUploadNewPost() throws InterruptedException {
        // Instantiate new post
        createNewPost();
        setupPostAttributes();

        // Upload new post to site
        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(mSite));

        assertNotSame(0, uploadedPost.getRemotePostId());
        assertEquals(false, uploadedPost.isLocalDraft());
    }

    public void testEditRemotePost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        final String dateCreated = uploadedPost.getDateCreated();

        uploadedPost.setTitle("From testEditingRemotePost");
        uploadedPost.setIsLocallyChanged(true);

        // Upload edited post
        uploadPost(uploadedPost);

        PostModel finalPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(mSite));

        assertEquals("From testEditingRemotePost", finalPost.getTitle());

        // The post should no longer be flagged as having local changes
        assertFalse(finalPost.isLocallyChanged());

        // The date created should not have been altered by the edits
        assertFalse(finalPost.getDateCreated().isEmpty());
        assertEquals(dateCreated, finalPost.getDateCreated());
    }

    public void testRevertLocallyChangedPost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        uploadedPost.setTitle("From testRevertingLocallyChangedPost");
        uploadedPost.setIsLocallyChanged(true);

        // Revert changes to post by replacing it with a fresh copy from the server
        fetchPost(uploadedPost);

        // Get the current copy of the post from the PostStore
        PostModel latestPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(mSite));

        assertEquals(POST_DEFAULT_TITLE, latestPost.getTitle());
        assertEquals(false, latestPost.isLocallyChanged());
    }

    public void testChangeLocalDraft() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        mPost.setTitle("From testChangingLocalDraft");

        // Save changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mPost.setTitle("From testChangingLocalDraft, redux");
        mPost.setContent("Some new content");
        mPost.setFeaturedImageId(7);

        // Save new changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(mSite));

        assertEquals("From testChangingLocalDraft, redux", mPost.getTitle());
        assertEquals("Some new content", mPost.getContent());
        assertEquals(7, mPost.getFeaturedImageId());
        assertEquals(false, mPost.isLocallyChanged());
        assertEquals(true, mPost.isLocalDraft());
    }

    public void testMultipleLocalChangesToUploadedPost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mPost.setTitle("From testMultipleLocalChangesToUploadedPost");
        mPost.setIsLocallyChanged(true);

        // Save changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mPost.setTitle("From testMultipleLocalChangesToUploadedPost, redux");
        mPost.setContent("Some different content");
        mPost.setFeaturedImageId(5);

        // Save new changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(mSite));

        assertEquals("From testMultipleLocalChangesToUploadedPost, redux", mPost.getTitle());
        assertEquals("Some different content", mPost.getContent());
        assertEquals(5, mPost.getFeaturedImageId());
        assertEquals(true, mPost.isLocallyChanged());
        assertEquals(false, mPost.isLocalDraft());
    }

    public void testChangePublishedPostToScheduled() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        String futureDate = "2075-10-14T10:51:11+00:00";
        uploadedPost.setDateCreated(futureDate);
        uploadedPost.setIsLocallyChanged(true);

        // Upload edited post
        uploadPost(uploadedPost);

        PostModel finalPost = mPostStore.getPostByLocalPostId(mPost.getId());

        // The post should no longer be flagged as having local changes
        assertFalse(finalPost.isLocallyChanged());

        // The post should now have a future created date and should have 'future' status
        assertEquals(futureDate, finalPost.getDateCreated());
        assertEquals(PostStatus.SCHEDULED, PostStatus.fromPost(finalPost));
    }

    public void testFetchPosts() throws InterruptedException {
        mNextEvent = TEST_EVENTS.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new PostStore.FetchPostsPayload(mSite, false)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int firstFetchPosts = mPostStore.getPostsCountForSite(mSite);

        // Dangerous, will fail for a site with no posts
        assertTrue(firstFetchPosts > 0 && firstFetchPosts <= PostStore.NUM_POSTS_PER_FETCH);
        assertEquals(mCanLoadMorePosts, firstFetchPosts == PostStore.NUM_POSTS_PER_FETCH);

        // Dependent on site having more than NUM_POSTS_TO_REQUEST posts
        assertTrue(mCanLoadMorePosts);

        mNextEvent = TEST_EVENTS.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new PostStore.FetchPostsPayload(mSite, true)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int currentStoredPosts = mPostStore.getPostsCountForSite(mSite);

        assertTrue(currentStoredPosts > firstFetchPosts);
        assertTrue(currentStoredPosts <= (PostStore.NUM_POSTS_PER_FETCH * 2));
    }

    public void testFetchPages() throws InterruptedException {
        mNextEvent = TEST_EVENTS.PAGES_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPagesAction(new PostStore.FetchPostsPayload(mSite, false)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int firstFetchPosts = mPostStore.getPagesCountForSite(mSite);

        // Dangerous, will fail for a site with no pages
        assertTrue(firstFetchPosts > 0 && firstFetchPosts <= PostStore.NUM_POSTS_PER_FETCH);
        assertEquals(mCanLoadMorePosts, firstFetchPosts == PostStore.NUM_POSTS_PER_FETCH);
    }

    public void testFullFeaturedPostUpload() throws InterruptedException {
        createNewPost();

        mPost.setTitle("A fully featured post");
        mPost.setContent("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.");
        String date = DateTimeUtils.iso8601UTCFromDate(new Date());
        mPost.setDateCreated(date);

        List<Long> categoryIds = new ArrayList<>(1);
        categoryIds.add((long) 1);
        mPost.setCategoryIdList(categoryIds);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        PostModel newPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(mSite));

        assertEquals("A fully featured post", newPost.getTitle());
        assertEquals("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.", newPost.getContent());
        assertEquals(date, newPost.getDateCreated());

        assertTrue(categoryIds.containsAll(newPost.getCategoryIdList()) &&
                newPost.getCategoryIdList().containsAll(categoryIds));
    }

    public void testFullFeaturedPageUpload() throws InterruptedException {
        createNewPost();

        mPost.setIsPage(true);

        mPost.setTitle("A fully featured page");
        mPost.setContent("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.");
        String date = DateTimeUtils.iso8601UTCFromDate(new Date());
        mPost.setDateCreated(date);

        mPost.setFeaturedImageId(77); // Not actually valid for pages

        uploadPost(mPost);

        // Get the current copy of the page from the PostStore
        PostModel newPage = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPagesCountForSite(mSite));

        assertNotSame(0, newPage.getRemotePostId());

        assertEquals("A fully featured page", newPage.getTitle());
        assertEquals("Some content here! <strong>Bold text</strong>.\r\n\r\nA new paragraph.", newPage.getContent());
        assertEquals(date, newPage.getDateCreated());

        assertEquals(0, newPage.getFeaturedImageId()); // The page should upload, but have the featured image stripped
    }

    public void testDeleteRemotePost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mNextEvent = TEST_EVENTS.POST_DELETED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(new RemotePostPayload(uploadedPost, mSite)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // The post should be removed from the db (regardless of whether it was deleted or just trashed on the server)
        assertEquals(null, mPostStore.getPostByLocalPostId(uploadedPost.getId()));
        assertEquals(0, WellSqlUtils.getTotalPostsCount());
        assertEquals(0, mPostStore.getPostsCountForSite(mSite));
    }

    // Error handling tests

    public void testFetchInvalidPost() throws InterruptedException {
        PostModel post = new PostModel();
        post.setRemotePostId(6420328);
        post.setRemoteSiteId(mSite.getSiteId());

        mNextEvent = TEST_EVENTS.ERROR_UNKNOWN_POST;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, mSite)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testEditInvalidPost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        String dateCreated = uploadedPost.getDateCreated();

        uploadedPost.setTitle("From testEditInvalidPost");
        uploadedPost.setIsLocallyChanged(true);

        savePost(uploadedPost);

        uploadedPost.setRemotePostId(289385);

        mNextEvent = TEST_EVENTS.ERROR_UNKNOWN_POST;
        mCountDownLatch = new CountDownLatch(1);

        // Upload edited post
        RemotePostPayload pushPayload = new RemotePostPayload(uploadedPost, mSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        PostModel persistedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(mSite));

        // The locally saved post should still be marked as locally changed, and local changes should be preserved
        assertEquals("From testEditInvalidPost", persistedPost.getTitle());
        assertTrue(persistedPost.isLocallyChanged());

        // The date created should not have been altered by the edit
        assertFalse(persistedPost.getDateCreated().isEmpty());
        assertEquals(dateCreated, persistedPost.getDateCreated());
    }

    public void testDeleteInvalidRemotePost() throws InterruptedException {
        PostModel invalidPost = new PostModel();
        invalidPost.setRemotePostId(6420328);
        invalidPost.setRemoteSiteId(mSite.getSiteId());

        mNextEvent = TEST_EVENTS.ERROR_UNKNOWN_POST;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(new RemotePostPayload(invalidPost, mSite)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchPostFromInvalidSite() throws InterruptedException {
        PostModel post = new PostModel();
        post.setRemotePostId(6420328);
        post.setRemoteSiteId(99999999999L);

        SiteModel site = new SiteModel();
        site.setIsWPCom(true);
        site.setSiteId(99999999999L);

        // Expecting a generic 404 error (unknown site)
        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, site)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void authenticate() throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AccountStore.AuthenticatePayload payload =
                new AccountStore.AuthenticatePayload(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                        BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSites() throws InterruptedException {
        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        assertEquals(false, event.isError());
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            return;
        }
        assertEquals(true, mSiteStore.hasSite());
        assertEquals(true, mSiteStore.hasWPComSite());
        assertEquals(TEST_EVENTS.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onPostChanged(OnPostChanged event) {
        AppLog.i(T.API, "Received OnPostChanged, cause: " + event.causeOfChange);
        if (event.isError()) {
            AppLog.i(T.API, "OnPostChanged has error: " + event.error.type + " - " + event.error.message);
            if (mNextEvent.equals(TEST_EVENTS.ERROR_UNKNOWN_POST)) {
                assertEquals(PostErrorType.UNKNOWN_POST, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_UNKNOWN_POST_TYPE)) {
                assertEquals(PostErrorType.UNKNOWN_POST_TYPE, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_GENERIC)) {
                assertEquals(PostErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
            }
            return;
        }
        switch (event.causeOfChange) {
            case UPDATE_POST:
                if (mNextEvent.equals(TEST_EVENTS.POST_UPDATED)) {
                    mCountDownLatch.countDown();
                }
                break;
            case FETCH_POSTS:
                if (mNextEvent.equals(TEST_EVENTS.POSTS_FETCHED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " posts, can load more: " + event.canLoadMore);
                    mCanLoadMorePosts = event.canLoadMore;
                    mCountDownLatch.countDown();
                }
                break;
            case FETCH_PAGES:
                if (mNextEvent.equals(TEST_EVENTS.PAGES_FETCHED)) {
                    AppLog.i(T.API, "Fetched " + event.rowsAffected + " pages, can load more: " + event.canLoadMore);
                    mCanLoadMorePosts = event.canLoadMore;
                    mCountDownLatch.countDown();
                }
                break;
            case DELETE_POST:
                if (mNextEvent.equals(TEST_EVENTS.POST_DELETED)) {
                    mCountDownLatch.countDown();
                }
                break;
        }
    }

    @Subscribe
    public void OnPostInstantiated(OnPostInstantiated event) {
        AppLog.i(T.API, "Received OnPostInstantiated");
        assertEquals(TEST_EVENTS.POST_INSTANTIATED, mNextEvent);

        assertEquals(true, event.post.isLocalDraft());
        assertEquals(0, event.post.getRemotePostId());
        assertNotSame(0, event.post.getId());
        assertNotSame(0, event.post.getLocalSiteId());

        mPost = event.post;
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onPostUploaded(OnPostUploaded event) {
        AppLog.i(T.API, "Received OnPostUploaded");
        if (event.isError()) {
            AppLog.i(T.API, "OnPostUploaded has error: " + event.error.type + " - " + event.error.message);
            if (mNextEvent.equals(TEST_EVENTS.ERROR_UNKNOWN_POST)) {
                assertEquals(PostErrorType.UNKNOWN_POST, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_UNKNOWN_POST_TYPE)) {
                assertEquals(PostErrorType.UNKNOWN_POST_TYPE, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_GENERIC)) {
                assertEquals(PostErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
            }
            return;
        }
        assertEquals(false, event.post.isLocalDraft());
        assertEquals(false, event.post.isLocallyChanged());
        assertNotSame(0, event.post.getRemotePostId());
    }

    private void setupPostAttributes() {
        mPost.setTitle(POST_DEFAULT_TITLE);
        mPost.setContent(POST_DEFAULT_DESCRIPTION);
    }

    private void createNewPost() throws InterruptedException {
        // Instantiate new post
        mNextEvent = TEST_EVENTS.POST_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);

        InstantiatePostPayload initPayload = new InstantiatePostPayload(mSite, false);
        mDispatcher.dispatch(PostActionBuilder.newInstantiatePostAction(initPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void uploadPost(PostModel post) throws InterruptedException {
        mNextEvent = TEST_EVENTS.POST_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        RemotePostPayload pushPayload = new RemotePostPayload(post, mSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchPost(PostModel post) throws InterruptedException {
        mNextEvent = TEST_EVENTS.POST_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, mSite)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void savePost(PostModel post) throws InterruptedException {
        mNextEvent = TEST_EVENTS.POST_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
