package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.InstantiatePostPayload;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostInstantiated;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
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

public class ReleaseStack_PostTestXMLRPC extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject PostStore mPostStore;

    private static final String POST_DEFAULT_TITLE = "PostTestXMLRPC base post";
    private static final String POST_DEFAULT_DESCRIPTION = "Hi there, I'm a post from FluxC!";
    private static final double EXAMPLE_LATITUDE = 44.8378;
    private static final double EXAMPLE_LONGITUDE = -0.5792;

    private CountDownLatch mCountDownLatch;
    private PostModel mPost;
    private static SiteModel sSite;

    private boolean mCanLoadMorePosts;

    private PostError mLastPostError;

    private enum TEST_EVENTS {
        NONE,
        POST_INSTANTIATED,
        POST_UPLOADED,
        POST_UPDATED,
        POSTS_FETCHED,
        PAGES_FETCHED,
        POST_DELETED,
        ERROR_UNKNOWN_POST,
        ERROR_UNKNOWN_POST_TYPE,
        ERROR_UNAUTHORIZED,
        ERROR_GENERIC
    }
    private TEST_EVENTS mNextEvent;

    {
        sSite = new SiteModel();
        sSite.setId(1);
        sSite.setSelfHostedSiteId(0);
        sSite.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE);
        sSite.setPassword(BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE);
        sSite.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);
    }

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

        mLastPostError = null;
    }

    public void testUploadNewPost() throws InterruptedException {
        // Instantiate new post
        createNewPost();
        setupPostAttributes();

        // Upload new post to site
        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

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
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

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
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

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
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

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
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

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

        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new PostStore.FetchPostsPayload(sSite, false)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int firstFetchPosts = mPostStore.getPostsCountForSite(sSite);

        // Dangerous, will fail for a site with no posts
        assertTrue(firstFetchPosts > 0 && firstFetchPosts <= PostStore.NUM_POSTS_PER_FETCH);
        assertEquals(mCanLoadMorePosts, firstFetchPosts == PostStore.NUM_POSTS_PER_FETCH);

        // Dependent on site having more than NUM_POSTS_TO_REQUEST posts
        assertTrue(mCanLoadMorePosts);

        mNextEvent = TEST_EVENTS.POSTS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new PostStore.FetchPostsPayload(sSite, true)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int currentStoredPosts = mPostStore.getPostsCountForSite(sSite);

        assertTrue(currentStoredPosts > firstFetchPosts);
        assertTrue(currentStoredPosts <= (PostStore.NUM_POSTS_PER_FETCH * 2));
    }

    public void testFetchPages() throws InterruptedException {
        mNextEvent = TEST_EVENTS.PAGES_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPagesAction(new PostStore.FetchPostsPayload(sSite, false)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        int firstFetchPosts = mPostStore.getPagesCountForSite(sSite);

        // Dangerous, will fail for a site with no pages
        assertTrue(firstFetchPosts > 0 && firstFetchPosts <= PostStore.NUM_POSTS_PER_FETCH);
        assertEquals(mCanLoadMorePosts, firstFetchPosts == PostStore.NUM_POSTS_PER_FETCH);
    }

    public void testFullFeaturedPostUpload() throws InterruptedException {
        createNewPost();

        mPost.setTitle("A fully featured post");
        mPost.setContent("Some content here! <strong>Bold text</strong>.");
        String date = DateTimeUtils.iso8601UTCFromDate(new Date());
        mPost.setDateCreated(date);

        List<Long> categoryIds = new ArrayList<>(1);
        categoryIds.add((long) 1);
        mPost.setCategoryIdList(categoryIds);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        PostModel newPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("A fully featured post", newPost.getTitle());
        assertEquals("Some content here! <strong>Bold text</strong>.", newPost.getContent());
        assertEquals(date, newPost.getDateCreated());

        assertTrue(categoryIds.containsAll(newPost.getCategoryIdList()) &&
                newPost.getCategoryIdList().containsAll(categoryIds));
    }

    public void testFullFeaturedPageUpload() throws InterruptedException {
        createNewPost();

        mPost.setIsPage(true);

        mPost.setTitle("A fully featured page");
        mPost.setContent("Some content here! <strong>Bold text</strong>.");
        String date = DateTimeUtils.iso8601UTCFromDate(new Date());
        mPost.setDateCreated(date);

        uploadPost(mPost);

        // Get the current copy of the page from the PostStore
        PostModel newPage = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPagesCountForSite(sSite));

        assertNotSame(0, newPage.getRemotePostId());

        assertEquals("A fully featured page", newPage.getTitle());
        assertEquals("Some content here! <strong>Bold text</strong>.", newPage.getContent());
        assertEquals(date, newPage.getDateCreated());
    }

    public void testAddLocationToRemotePost() throws InterruptedException {
        // 1. Upload a post with no location data
        createNewPost();

        mPost.setTitle("A post with location");
        mPost.setContent("Some content");

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("A post with location", mPost.getTitle());
        assertEquals("Some content", mPost.getContent());

        // The post should not have a location since we never set one
        assertEquals(false, mPost.hasLocation());

        // 2. Modify the post, setting some location data
        mPost.setLocation(EXAMPLE_LATITUDE, EXAMPLE_LONGITUDE);
        mPost.setIsLocallyChanged(true);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        // The set location should be stored in the remote post
        assertEquals(true, mPost.hasLocation());
        assertEquals(EXAMPLE_LATITUDE, mPost.getLocation().getLatitude());
        assertEquals(EXAMPLE_LONGITUDE, mPost.getLocation().getLongitude());
    }

    public void testUploadPostWithLocation() throws InterruptedException {
        // 1. Upload a post with location data
        createNewPost();

        mPost.setTitle("A post with location");
        mPost.setContent("Some content");

        mPost.setLocation(EXAMPLE_LATITUDE, EXAMPLE_LONGITUDE);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        assertEquals("A post with location", mPost.getTitle());
        assertEquals("Some content", mPost.getContent());

        // The set location should be stored in the remote post
        assertEquals(true, mPost.hasLocation());
        assertEquals(EXAMPLE_LATITUDE, mPost.getLocation().getLatitude());
        assertEquals(EXAMPLE_LONGITUDE, mPost.getLocation().getLongitude());

        // 2. Modify the post without changing the location data and update
        mPost.setTitle("A new title");
        mPost.setIsLocallyChanged(true);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals("A new title", mPost.getTitle());

        // The location data should not have been altered
        assertEquals(true, mPost.hasLocation());
        assertEquals(EXAMPLE_LATITUDE, mPost.getLocation().getLatitude());
        assertEquals(EXAMPLE_LONGITUDE, mPost.getLocation().getLongitude());

        // 3. Clear location data from the post and update
        mPost.clearLocation();
        mPost.setIsLocallyChanged(true);

        uploadPost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        // The post should not have a location anymore
        assertEquals(false, mPost.hasLocation());
    }

    public void testDeleteRemotePost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mNextEvent = TEST_EVENTS.POST_DELETED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(new RemotePostPayload(uploadedPost, sSite)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // The post should be removed from the db (regardless of whether it was deleted or just trashed on the server)
        assertEquals(null, mPostStore.getPostByLocalPostId(uploadedPost.getId()));
        assertEquals(0, WellSqlUtils.getTotalPostsCount());
        assertEquals(0, mPostStore.getPostsCountForSite(sSite));
    }

    // Error handling tests

    public void testFetchInvalidPost() throws InterruptedException {
        PostModel post = new PostModel();
        post.setRemotePostId(6420328);

        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, sSite)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_POST error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid post ID.", mLastPostError.message);
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

        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        // Upload edited post
        RemotePostPayload pushPayload = new RemotePostPayload(uploadedPost, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        PostModel persistedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        // The locally saved post should still be marked as locally changed, and local changes should be preserved
        assertEquals("From testEditInvalidPost", persistedPost.getTitle());
        assertTrue(persistedPost.isLocallyChanged());

        // The date created should not have been altered by the edit
        assertFalse(persistedPost.getDateCreated().isEmpty());
        assertEquals(dateCreated, persistedPost.getDateCreated());

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_POST error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid post ID.", mLastPostError.message);
    }

    public void testDeleteInvalidRemotePost() throws InterruptedException {
        PostModel invalidPost = new PostModel();
        invalidPost.setRemotePostId(6420328);

        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(new RemotePostPayload(invalidPost, sSite)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_POST error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid post ID.", mLastPostError.message);
    }

    public void testCreateNewPostWithInvalidCategory() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        List<Long> categories = new ArrayList<>();
        categories.add((long) 999999);

        mPost.setCategoryIdList(categories);

        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        // Upload edited post
        RemotePostPayload pushPayload = new RemotePostPayload(mPost, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_TERM error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid term ID.", mLastPostError.message);
    }

    public void testCreateNewPostWithInvalidTag() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        List<Long> tags = new ArrayList<>();
        tags.add((long) 999999);

        mPost.setTagIdList(tags);

        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        // Upload edited post
        RemotePostPayload pushPayload = new RemotePostPayload(mPost, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_TERM error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid term ID.", mLastPostError.message);
    }

    public void testEditPostWithInvalidTerm() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        String dateCreated = uploadedPost.getDateCreated();

        uploadedPost.setTitle("From testEditInvalidPost");
        uploadedPost.setIsLocallyChanged(true);

        savePost(uploadedPost);

        List<Long> categories = new ArrayList<>();
        categories.add((long) 999999);

        uploadedPost.setCategoryIdList(categories);

        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        // Upload edited post
        RemotePostPayload pushPayload = new RemotePostPayload(uploadedPost, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        PostModel persistedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        // The locally saved post should still be marked as locally changed, and local changes should be preserved
        assertEquals("From testEditInvalidPost", persistedPost.getTitle());
        assertTrue(persistedPost.isLocallyChanged());

        // The date created should not have been altered by the edit
        assertFalse(persistedPost.getDateCreated().isEmpty());
        assertEquals(dateCreated, persistedPost.getDateCreated());

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_TERM error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid term ID.", mLastPostError.message);
    }

    public void testCreateNewPostWithInvalidFeaturedImage() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        mPost.setFeaturedImageId(999999);

        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        // Upload edited post
        RemotePostPayload pushPayload = new RemotePostPayload(mPost, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_ATTACHMENT error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid attachment ID.", mLastPostError.message);
    }

    public void testEditPostWithInvalidFeaturedImage() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        String dateCreated = uploadedPost.getDateCreated();

        uploadedPost.setTitle("From testEditInvalidPost");
        uploadedPost.setIsLocallyChanged(true);

        savePost(uploadedPost);

        uploadedPost.setFeaturedImageId(999999);

        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        // Upload edited post
        RemotePostPayload pushPayload = new RemotePostPayload(uploadedPost, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        PostModel persistedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(sSite));

        // The locally saved post should still be marked as locally changed, and local changes should be preserved
        assertEquals("From testEditInvalidPost", persistedPost.getTitle());
        assertTrue(persistedPost.isLocallyChanged());

        // The date created should not have been altered by the edit
        assertFalse(persistedPost.getDateCreated().isEmpty());
        assertEquals(dateCreated, persistedPost.getDateCreated());

        // TODO: This will fail for non-English sites - we should be checking for an UNKNOWN_ATTACHMENT error instead
        // (once we make the fixes needed for PostXMLRPCClient to correctly identify post errors)
        assertEquals("Invalid attachment ID.", mLastPostError.message);
    }

    public void testFetchPostBadCredentials() throws InterruptedException {
        PostModel post = new PostModel();
        post.setRemotePostId(10);

        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        SiteModel badSite = new SiteModel();
        badSite.setId(2);
        badSite.setSelfHostedSiteId(0);
        badSite.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE);
        badSite.setPassword("wrong");
        badSite.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, badSite)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("Incorrect username or password.", mLastPostError.message);
    }

    public void testFetchPostBadUrl() throws InterruptedException {
        PostModel post = new PostModel();
        post.setRemotePostId(10);

        mNextEvent = TEST_EVENTS.ERROR_GENERIC;
        mCountDownLatch = new CountDownLatch(1);

        SiteModel badSite = new SiteModel();
        badSite.setId(2);
        badSite.setSelfHostedSiteId(0);
        badSite.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE);
        badSite.setPassword("wrong");
        badSite.setXmlRpcUrl("http://www.android.com");

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, badSite)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    // TODO: Test: Upload a page to a custom site that has pages disabled (should get a 403 'Invalid post type')

    public void testFetchPostsAsSubscriber() throws InterruptedException {
        SiteModel site = new SiteModel();
        site.setId(2);
        site.setSelfHostedSiteId(0);
        site.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE_SUBSCRIBER);
        site.setPassword(BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE_SUBSCRIBER);
        site.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE_SUBSCRIBER_ENDPOINT);

        // Expecting a 401 error (authorization required)
        mNextEvent = TEST_EVENTS.ERROR_UNAUTHORIZED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(new PostStore.FetchPostsPayload(site)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testCreatePostAsSubscriber() throws InterruptedException {
        SiteModel subscriberSite = new SiteModel();
        subscriberSite.setId(2);
        subscriberSite.setSelfHostedSiteId(0);
        subscriberSite.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE_SUBSCRIBER);
        subscriberSite.setPassword(BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE_SUBSCRIBER);
        subscriberSite.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE_SUBSCRIBER_ENDPOINT);

        // Instantiate new post
        mNextEvent = TEST_EVENTS.POST_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);

        InstantiatePostPayload initPayload = new InstantiatePostPayload(subscriberSite, false);
        mDispatcher.dispatch(PostActionBuilder.newInstantiatePostAction(initPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        setupPostAttributes();

        // Attempt to upload new post to site
        mNextEvent = TEST_EVENTS.ERROR_UNAUTHORIZED;
        mCountDownLatch = new CountDownLatch(1);

        RemotePostPayload pushPayload = new RemotePostPayload(mPost, subscriberSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("Sorry, you are not allowed to post on this site.", mLastPostError.message);

        PostModel failedUploadPost = mPostStore.getPostByLocalPostId(mPost.getId());

        // Post should still exist locally, but not marked as uploaded
        assertEquals(1, WellSqlUtils.getTotalPostsCount());
        assertEquals(1, mPostStore.getPostsCountForSite(subscriberSite));
        assertEquals(0, mPostStore.getUploadedPostsCountForSite(subscriberSite));

        assertEquals(0, failedUploadPost.getRemotePostId());
        assertEquals(true, failedUploadPost.isLocalDraft());
    }

    @Subscribe
    public void onPostChanged(OnPostChanged event) {
        AppLog.i(T.API, "Received OnPostChanged, causeOfChange: " + event.causeOfChange);
        if (event.isError()) {
            AppLog.i(T.API, "OnPostChanged has error: " + event.error.type + " - " + event.error.message);
            mLastPostError = event.error;
            if (mNextEvent.equals(TEST_EVENTS.ERROR_UNKNOWN_POST)) {
                assertEquals(PostErrorType.UNKNOWN_POST, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_UNKNOWN_POST_TYPE)) {
                assertEquals(PostErrorType.UNKNOWN_POST_TYPE, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_UNAUTHORIZED)) {
                assertEquals(PostErrorType.UNAUTHORIZED, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_GENERIC)) {
                assertEquals(PostErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected error with type: " + event.error.type);
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
        if (event.isError()) {
            throw new AssertionError("Unexpected error with type: " + event.error.type);
        }
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
            mLastPostError = event.error;
            if (mNextEvent.equals(TEST_EVENTS.ERROR_UNKNOWN_POST)) {
                assertEquals(PostErrorType.UNKNOWN_POST, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_UNKNOWN_POST_TYPE)) {
                assertEquals(PostErrorType.UNKNOWN_POST_TYPE, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_UNAUTHORIZED)) {
                assertEquals(PostErrorType.UNAUTHORIZED, event.error.type);
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TEST_EVENTS.ERROR_GENERIC)) {
                assertEquals(PostErrorType.GENERIC_ERROR, event.error.type);
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected error with type: " + event.error.type);
            }
            return;
        }
        assertEquals(TEST_EVENTS.POST_UPLOADED, mNextEvent);
        assertEquals(false, event.post.isLocalDraft());
        assertEquals(false, event.post.isLocallyChanged());
        assertNotSame(0, event.post.getRemotePostId());

        mCountDownLatch.countDown();
    }

    private void setupPostAttributes() {
        mPost.setTitle(POST_DEFAULT_TITLE);
        mPost.setContent(POST_DEFAULT_DESCRIPTION);
    }

    private void createNewPost() throws InterruptedException {
        // Instantiate new post
        mNextEvent = TEST_EVENTS.POST_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);

        InstantiatePostPayload initPayload = new InstantiatePostPayload(sSite, false);
        mDispatcher.dispatch(PostActionBuilder.newInstantiatePostAction(initPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void uploadPost(PostModel post) throws InterruptedException {
        mNextEvent = TEST_EVENTS.POST_UPLOADED;
        mCountDownLatch = new CountDownLatch(1);

        RemotePostPayload pushPayload = new RemotePostPayload(post, sSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchPost(PostModel post) throws InterruptedException {
        mNextEvent = TEST_EVENTS.POST_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, sSite)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void savePost(PostModel post) throws InterruptedException {
        mNextEvent = TEST_EVENTS.POST_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
