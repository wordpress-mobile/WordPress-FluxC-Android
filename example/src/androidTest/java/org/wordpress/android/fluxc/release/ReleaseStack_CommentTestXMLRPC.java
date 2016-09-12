package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_CommentTestXMLRPC extends ReleaseStack_XMLRPCBase {
    @Inject CommentStore mCommentStore;

    private enum TEST_EVENTS {
        NONE,
        COMMENT_CHANGED,
    }
    private TEST_EVENTS mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Authenticate, fetch sites and initialize mSite.
        init();
        mNextEvent = TEST_EVENTS.NONE;
    }

    public void testFetchComments() throws InterruptedException {
        FetchCommentsPayload payload = new FetchCommentsPayload(mSite);
        mNextEvent = TEST_EVENTS.COMMENT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(payload));
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onCommentChanged(CommentStore.OnCommentChanged event) {
        List<CommentModel> comments = mCommentStore.getCommentsForSite(mSite);
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            return;
        }
        AppLog.i(T.TESTS, "comments count " + comments.size());
        assertTrue(comments.size() != 0);
        assertEquals(TEST_EVENTS.COMMENT_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

}
