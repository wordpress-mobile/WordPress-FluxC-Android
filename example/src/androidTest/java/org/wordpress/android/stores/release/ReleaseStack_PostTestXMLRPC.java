package org.wordpress.android.stores.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.TestUtils;
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

    CountDownLatch mCountDownLatch;

    enum TEST_EVENTS {
        NONE,
        POST_INSTANTIATED,
        POST_UPLOADED
    }
    private TEST_EVENTS mNextEvent;

    private PostModel mPost;
    private SiteModel mSite;

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
        mNextEvent = TEST_EVENTS.POST_INSTANTIATED;
        mCountDownLatch = new CountDownLatch(1);

        PostStore.InstantiatePostPayload initPayload = new PostStore.InstantiatePostPayload(mSite, false);
        mDispatcher.dispatch(PostActionBuilder.newInstantiatePostAction(initPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Upload new post to site
        mPost.setTitle("From testUploadNewPost");
        mPost.setDescription("Hi there, I'm a post from FluxC!");
        mPost.setFeaturedImageId(0);

        mNextEvent = TEST_EVENTS.POST_UPLOADED;
        mCountDownLatch = new CountDownLatch(1);

        PostStore.ChangePostPayload pushPayload = new PostStore.ChangePostPayload(mPost, mSite);
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(pushPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        PostModel uploadedPost = mPostStore.getPostByLocalPostId(mPost.getId());

        assertNotSame(0, uploadedPost.getRemotePostId());
        assertEquals(false, uploadedPost.isLocalDraft());
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

    // TODO: Test making local changes, then reverting them, and check that (FETCH_POST) works correctly and syncs with the server

    @Subscribe
    public void onPostUploaded(PostStore.OnPostUploaded event) {
        assertEquals(TEST_EVENTS.POST_UPLOADED, mNextEvent);
        assertEquals(false, event.post.isLocalDraft());
        assertEquals(false, event.post.isLocallyChanged());
        assertNotSame(0, event.post.getRemotePostId());

        mPost = event.post;

        mCountDownLatch.countDown();
    }
}
