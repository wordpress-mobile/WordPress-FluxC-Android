package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.junit.After;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.ActivityAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.generated.ActivityActionBuilder;
import org.wordpress.android.fluxc.model.activity.RewindStatusModel;
import org.wordpress.android.fluxc.store.ActivityLogStore;
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivitiesPayload;
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivitiesPayload;
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedRewindStatePayload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class ReleaseStack_ActivityLogTestWPCom extends ReleaseStack_WPComBase {
    @Inject ActivityLogStore mActivityLogStore;

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
        FetchActivitiesPayload payload = new FetchActivitiesPayload(sSite, numOfActivitiesRequested, 0);
        mActivityLogStore.onAction(ActivityActionBuilder.newFetchActivitiesAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mIncomingActions.size() == 1);
        assertTrue(((FetchedActivitiesPayload) mIncomingActions.get(0).getPayload())
                           .getActivityLogModelRespons()
                           .size() == numOfActivitiesRequested);
    }

    @Test
    public void testFetchRewindState() throws InterruptedException {
        this.mCountDownLatch = new CountDownLatch(1);
        mActivityLogStore.onAction(ActivityActionBuilder
                .newFetchRewindStateAction(new ActivityLogStore.FetchRewindStatePayload(sSite, 10, 0)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mIncomingActions.size() == 1);
        RewindStatusModel rewindStatusModelResponse =
                ((FetchedRewindStatePayload) mIncomingActions.get(0).getPayload()).getRewindStatusModelResponse();
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
