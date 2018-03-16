package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.junit.After;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.ActivityAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.generated.ActivityActionBuilder;
import org.wordpress.android.fluxc.model.activity.RewindStatusModel;
import org.wordpress.android.fluxc.store.ActivityStore;
import org.wordpress.android.fluxc.store.FetchActivitiesPayload;
import org.wordpress.android.fluxc.store.FetchRewindStatePayload;
import org.wordpress.android.fluxc.store.FetchRewindStateResponsePayload;
import org.wordpress.android.fluxc.store.FetchedActivitiesPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class ReleaseStack_ActivityTestWPCom extends ReleaseStack_WPComBase {
    @Inject ActivityStore mActivityStore;

    private CountDownLatch mBackup;
    private List<Action> mIncomingActions;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        init();
        this.mBackup = this.mCountDownLatch;
        this.mIncomingActions = new ArrayList<>();
    }

    @Test
    public void testFetchActivities() throws InterruptedException {
        this.mCountDownLatch = new CountDownLatch(1);
        int numOfActivitiesRequested = 1;
        mActivityStore.onAction(
                ActivityActionBuilder.newFetchActivitiesAction(new FetchActivitiesPayload(sSite, numOfActivitiesRequested, 0)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mIncomingActions.size() == 1);
        assertTrue(((FetchedActivitiesPayload) mIncomingActions.get(0).getPayload())
                           .getActivityModelRespons()
                           .size() == numOfActivitiesRequested);
    }

    @Test
    public void testFetchRewindState() throws InterruptedException {
        this.mCountDownLatch = new CountDownLatch(1);
        mActivityStore.onAction(ActivityActionBuilder
                .newFetchRewindStateAction(new FetchRewindStatePayload(sSite, 10, 0)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mIncomingActions.size() == 1);
        RewindStatusModel rewindStatusModelResponse =
                ((FetchRewindStateResponsePayload) mIncomingActions.get(0).getPayload()).getRewindStatusModelResponse();
        assertNotNull(rewindStatusModelResponse);
        assertNotNull(rewindStatusModelResponse.getState());
    }

    @After
    public void tearDown() throws Exception {
        this.mCountDownLatch = mBackup;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAction(Action action) {
        if (action.getType() instanceof ActivityAction) {
            if (mIncomingActions != null) {
                mIncomingActions.add(action);
                mCountDownLatch.countDown();
            }
        }
    }
}
