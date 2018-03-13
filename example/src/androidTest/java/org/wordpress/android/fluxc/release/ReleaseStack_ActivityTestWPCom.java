package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.generated.ActivityActionBuilder;
import org.wordpress.android.fluxc.store.ActivityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_ActivityTestWPCom extends ReleaseStack_WPComBase {
    @Inject ActivityStore mActivityStore;

    private CountDownLatch mBackup;
    private List<Action> mIncomingActivities;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        init();
        this.mBackup = this.mCountDownLatch;
        this.mIncomingActivities = new ArrayList<>();
    }

    public void testFetchActivities() throws InterruptedException {
        this.mCountDownLatch = new CountDownLatch(1);
        mActivityStore.onAction(ActivityActionBuilder.newFetchActivitiesAction(new ActivityStore.FetchActivitiesPayload(sSite, 10, 0)));
        assertTrue(this.mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mIncomingActivities.size() == 1);
        assertTrue(((ActivityStore.FetchActivitiesResponsePayload) mIncomingActivities.get(0).getPayload()).getActivities().size() == 1);
    }

    @Override
    public void tearDown() throws Exception {
        this.mCountDownLatch = mBackup;
        super.tearDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAction(Action action) {
        if (mIncomingActivities != null) {
            mCountDownLatch.countDown();
            mIncomingActivities.add(action);
        }
    }
}
