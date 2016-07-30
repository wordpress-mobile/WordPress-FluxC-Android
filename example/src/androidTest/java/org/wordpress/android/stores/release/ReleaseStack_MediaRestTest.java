package org.wordpress.android.stores.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.TestUtils;
import org.wordpress.android.stores.action.MediaAction;
import org.wordpress.android.stores.example.BuildConfig;
import org.wordpress.android.stores.generated.AuthenticationActionBuilder;
import org.wordpress.android.stores.generated.MediaActionBuilder;
import org.wordpress.android.stores.generated.SiteActionBuilder;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.MediaStore;
import org.wordpress.android.stores.store.SiteStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_MediaRestTest extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    enum TEST_EVENTS {
        FETCHED_ALL_MEDIA,
        FETCHED_KNOWN_IMAGES
    }

    private TEST_EVENTS mExpectedEvent;
    private CountDownLatch mCountDownLatch;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        mDispatcher.register(this);
    }

    @Override
    public void tearDown() throws Exception {
        mDispatcher.unregister(this);
        super.tearDown();
    }

    public void testFetchAllMedia() throws InterruptedException {
        loginAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        SiteModel site = mSiteStore.getSites().get(0);
        MediaStore.FetchMediaPayload fetchPayload = new MediaStore.FetchMediaPayload(site, null);
        mExpectedEvent = TEST_EVENTS.FETCHED_ALL_MEDIA;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newFetchAllMediaAction(fetchPayload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchSpecificMedia() throws InterruptedException {
        loginAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        String knownImageIds = BuildConfig.TEST_WPCOM_IMAGE_IDS_TEST1;
        String[] splitIds = knownImageIds.split(",");
        List<Long> idList = new ArrayList<>();
        for (String id : splitIds) {
            idList.add(Long.valueOf(id));
        }
        SiteModel site = mSiteStore.getSites().get(0);
        MediaStore.FetchMediaPayload payload = new MediaStore.FetchMediaPayload(site, idList);
        mExpectedEvent = TEST_EVENTS.FETCHED_KNOWN_IMAGES;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(payload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (event.causeOfChange == MediaAction.FETCH_ALL_MEDIA) {
            assertEquals(TEST_EVENTS.FETCHED_ALL_MEDIA, mExpectedEvent);
        } else if (event.causeOfChange == MediaAction.FETCH_MEDIA) {
            if (eventHasKnownImages(event)) {
                assertEquals(TEST_EVENTS.FETCHED_KNOWN_IMAGES, mExpectedEvent);
            }
        }
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        assertEquals(false, event.isError);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        assertEquals(true, mSiteStore.hasDotComSite());
        mCountDownLatch.countDown();
    }

    private void loginAndFetchSites(String username, String password) throws InterruptedException {
        AccountStore.AuthenticatePayload payload =
                new AccountStore.AuthenticatePayload(username, password);

        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private boolean eventHasKnownImages(MediaStore.OnMediaChanged event) {
        if (event == null || event.media == null || event.media.isEmpty()) return false;
        String[] splitIds = BuildConfig.TEST_WPCOM_IMAGE_IDS_TEST1.split(",");
        if (splitIds.length != event.media.size()) return false;
        for (String id : splitIds) {
            if (!event.media.contains(Long.valueOf(id))) return false;
        }
        return true;
    }
}
