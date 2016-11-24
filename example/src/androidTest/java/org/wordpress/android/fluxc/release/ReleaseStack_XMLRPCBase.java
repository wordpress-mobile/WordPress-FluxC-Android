package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_XMLRPCBase extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;

    protected CountDownLatch mCountDownLatch;
    protected SiteModel mSite;

    private enum TestEvents {
        NONE,
        SITE_CHANGED,
    }
    private TestEvents mNextEvent;


    protected void init() throws Exception {
        // Register
        mNextEvent = TestEvents.NONE;

        mDispatcher.register(this);

        if (mSite == null) {
            fetchSites();
            mSite = mSiteStore.getSites().get(0);
        }
    }

    private void fetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT;
        mNextEvent = TestEvents.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("event error type: " + event.error.type);
        }
        assertTrue(mSiteStore.hasSite());
        assertTrue(mSiteStore.hasSelfHostedSite());
        assertEquals(TestEvents.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }
}
