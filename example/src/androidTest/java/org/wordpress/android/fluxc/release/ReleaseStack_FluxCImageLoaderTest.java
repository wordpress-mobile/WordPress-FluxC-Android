package org.wordpress.android.fluxc.release;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ReleaseStack_FluxCImageLoaderTest extends ReleaseStack_Base {
    @Inject SiteStore mSiteStore;
    @Inject MediaStore mMediaStore;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject FluxCImageLoader mFluxCImageLoader;

    enum TestEvents {
        NONE,
        SITE_CHANGED,
        FETCHED_MEDIA_LIST
    }

    private TestEvents mNextEvent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
    }

    @Test
    public void testLoadImageFromHTTPAuthSite() throws Throwable {
        signInToHTTPAuthSite();

        final SiteModel site = mSiteStore.getSites().get(0);

        // Fetch media list and verify store is not empty
        fetchSiteMedia(site);

        // Download one of the media items
        final String imageUrl = mMediaStore.getAllSiteMedia(site).get(0).getUrl();
        mCountDownLatch = new CountDownLatch(1);

        Runnable loadImageTask = new Runnable() {
            @Override
            public void run() {
                mFluxCImageLoader.get(imageUrl,
                        new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                                if (isImmediate) {
                                    // Network request hasn't happened yet, keep waiting
                                    return;
                                }
                                assertNotNull(response.getBitmap());
                                mCountDownLatch.countDown();
                            }

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                AppLog.e(T.TESTS, "Failed to download image: " + error.getMessage());
                                fail("Image request failed!");
                            }
                        }
                );
            }
        };
        getInstrumentation().runOnMainSync(loadImageTask);

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertTrue(mSiteStore.hasSite());
        assertEquals(TestEvents.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaListFetched(OnMediaListFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.FETCHED_MEDIA_LIST, mNextEvent);
        mCountDownLatch.countDown();
    }

    private void signInToHTTPAuthSite() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH_ENDPOINT;
        mNextEvent = TestEvents.SITE_CHANGED;
        // Set known HTTP Auth credentials
        mHTTPAuthManager.addHTTPAuthCredentials(BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH, BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH_ENDPOINT,
                null);

        mCountDownLatch = new CountDownLatch(1);
        // Retry to fetch sites,we expect a site refresh
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSiteMedia(SiteModel site) throws InterruptedException {
        mNextEvent = TestEvents.FETCHED_MEDIA_LIST;
        mCountDownLatch = new CountDownLatch(1);

        FetchMediaListPayload fetchPayload = new FetchMediaListPayload(
                site, MediaStore.DEFAULT_NUM_MEDIA_PER_FETCH, false);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(fetchPayload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mMediaStore.getAllSiteMedia(site).isEmpty());
    }
}

