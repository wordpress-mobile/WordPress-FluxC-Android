package org.wordpress.android.stores.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.TestUtils;
import org.wordpress.android.stores.action.PostAction;
import org.wordpress.android.stores.example.BuildConfig;
import org.wordpress.android.stores.generated.PostActionBuilder;
import org.wordpress.android.stores.model.PostModel;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.store.PostStore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_PostTestXMLRPC extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject PostStore mPostStore;

    private static final String POST_DEFAULT_TITLE = "PostTextXMLRPC base post";
    private static final String POST_DEFAULT_DESCRIPTION = "Hi there, I'm a post from FluxC!";

    private CountDownLatch mCountDownLatch;
    private PostModel mPost;
    private SiteModel mSite;

    enum TEST_EVENTS {
        NONE,
        POST_INSTANTIATED,
        POST_UPLOADED,
        POST_UPDATED
    }
    private TEST_EVENTS mNextEvent;

    {
        mSite = new SiteModel();
        mSite.setId(1);
        mSite.setDotOrgSiteId(0);
        mSite.setUsername(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE);
        mSite.setPassword(BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE);
        mSite.setXmlRpcUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);
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
    }

    public void testUploadNewPost() throws InterruptedException {
        // Instantiate new post
        createNewPost();
        setupPostAttributes();

        // Upload new post to site
        uploadPost(mPost);

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertNotSame(0, uploadedPost.getRemotePostId());
        assertEquals(false, uploadedPost.isLocalDraft());
    }

    public void testEditingRemotePost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        mPost.setTitle("From testEditingRemotePost");
        mPost.setIsLocallyChanged(true);

        // Upload edited post
        uploadPost(mPost);

        assertEquals("From testEditingRemotePost", mPost.getTitle());
    }

    public void testRevertingLocallyChangedPost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        mPost.setTitle("From testRevertingLocallyChangedPost");
        mPost.setIsLocallyChanged(true);

        // Revert changes to post by replacing it with a fresh copy from the server
        fetchPost(mPost);

        // Get the current copy of the post from the PostStore
        PostModel latestPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals(POST_DEFAULT_TITLE, latestPost.getTitle());
        assertEquals(false, latestPost.isLocallyChanged());
    }

    public void testChangingLocalDraft() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        mPost.setTitle("From testChangingLocalDraft");

        // Save changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mPost.setTitle("From testChangingLocalDraft, redux");
        mPost.setDescription("Some new content");
        mPost.setFeaturedImageId(7);

        // Save new changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals("From testChangingLocalDraft, redux", mPost.getTitle());
        assertEquals("Some new content", mPost.getDescription());
        assertEquals(7, mPost.getFeaturedImageId());
        assertEquals(false, mPost.isLocallyChanged());
        assertEquals(true, mPost.isLocalDraft());
    }

    public void testMultipleLocalChangesToUploadedPost() throws InterruptedException {
        createNewPost();
        setupPostAttributes();

        uploadPost(mPost);

        mPost.setTitle("From testMultipleLocalChangesToUploadedPost");
        mPost.setIsLocallyChanged(true);

        // Save changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        mPost.setTitle("From testMultipleLocalChangesToUploadedPost, redux");
        mPost.setDescription("Some different content");
        mPost.setFeaturedImageId(5);

        // Save new changes locally
        savePost(mPost);

        // Get the current copy of the post from the PostStore
        mPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertEquals("From testMultipleLocalChangesToUploadedPost, redux", mPost.getTitle());
        assertEquals("Some different content", mPost.getDescription());
        assertEquals(5, mPost.getFeaturedImageId());
        assertEquals(true, mPost.isLocallyChanged());
        assertEquals(false, mPost.isLocalDraft());
    }

    // TEST full set of post params assigned, uploaded, and verified after fetch

    @Subscribe
    public void onPostChanged(PostStore.OnPostChanged event) {
        if (event.causeOfChange.equals(PostAction.UPDATE_POST)) {
            if (mCountDownLatch.getCount() > 0) {
                assertEquals(TEST_EVENTS.POST_UPDATED, mNextEvent);
                mCountDownLatch.countDown();
            }
        }
    }

    @Subscribe
    public void OnPostInstantiated(PostStore.OnPostInstantiated event) {
        assertEquals(TEST_EVENTS.POST_INSTANTIATED, mNextEvent);

        assertEquals(true, event.post.isLocalDraft());
        assertEquals(0, event.post.getRemotePostId());
        assertNotSame(0, event.post.getId());
        assertNotSame(0, event.post.getLocalSiteId());

        mPost = event.post;
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onPostUploaded(PostStore.OnPostUploaded event) {
        assertEquals(TEST_EVENTS.POST_UPLOADED, mNextEvent);
        assertEquals(false, event.post.isLocalDraft());
        assertEquals(false, event.post.isLocallyChanged());
        assertNotSame(0, event.post.getRemotePostId());

        mPost = event.post;

        mCountDownLatch.countDown();
    }

    private void setupPostAttributes() {
        mPost.setTitle(POST_DEFAULT_TITLE);
        mPost.setDescription(POST_DEFAULT_DESCRIPTION);
        mPost.setFeaturedImageId(0);
    }

    private void createNewPost() throws InterruptedException {
        // Instantiate new post
        mNextEvent = TEST_EVENTS.POST_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);

        PostStore.InstantiatePostPayload initPayload = new PostStore.InstantiatePostPayload(mSite, false);
        mDispatcher.dispatch(PostActionBuilder.newInstantiatePostAction(initPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void uploadPost(PostModel post) throws InterruptedException {
        mNextEvent = TEST_EVENTS.POST_UPLOADED;
        mCountDownLatch = new CountDownLatch(1);

        PostStore.ChangePostPayload pushPayload = new PostStore.ChangePostPayload(post, mSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchPost(PostModel post) throws InterruptedException {
        mNextEvent = TEST_EVENTS.POST_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new PostStore.FetchPostPayload(post, mSite)));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void savePost(PostModel post) throws InterruptedException {
        mNextEvent = TEST_EVENTS.POST_UPDATED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
