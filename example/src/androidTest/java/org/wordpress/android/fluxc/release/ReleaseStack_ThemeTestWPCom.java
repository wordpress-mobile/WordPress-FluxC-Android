package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.fluxc.store.ThemeStore.FetchWPComThemesPayload;
import org.wordpress.android.fluxc.store.ThemeStore.OnCurrentThemeFetched;
import org.wordpress.android.fluxc.store.ThemeStore.OnSiteThemesChanged;
import org.wordpress.android.fluxc.store.ThemeStore.OnStarterDesignsFetched;
import org.wordpress.android.fluxc.store.ThemeStore.OnThemeActivated;
import org.wordpress.android.fluxc.store.ThemeStore.OnWpComThemesChanged;
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("NewClassNamingConvention")
public class ReleaseStack_ThemeTestWPCom extends ReleaseStack_WPComBase {
    enum TestEvents {
        NONE,
        FETCHED_WPCOM_THEMES,
        FETCHED_SITE_THEMES,
        FETCHED_CURRENT_THEME,
        ACTIVATED_THEME,
        FETCHED_STARTER_DESIGNS
    }

    @Inject ThemeStore mThemeStore;

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
    public void testFetchCurrentTheme() throws InterruptedException {
        // Make sure no theme is active at first
        assertNull(mThemeStore.getActiveThemeForSite(sSite));
        ThemeModel currentTheme = fetchCurrentTheme();
        assertNotNull(currentTheme);
    }

    @Test
    public void testFetchWPComThemes() throws InterruptedException {
        // verify themes don't already exist in store
        assertTrue(mThemeStore.getWpComThemes().isEmpty());
        fetchWpComThemes();
        // verify response received and WP themes list is not empty
        assertFalse(mThemeStore.getWpComThemes().isEmpty());

        // verify that we have the 3 mobile-friendly categories being non empty
        for (String category : new String[]{ThemeStore.MOBILE_FRIENDLY_CATEGORY_BLOG,
                ThemeStore.MOBILE_FRIENDLY_CATEGORY_WEBSITE, ThemeStore.MOBILE_FRIENDLY_CATEGORY_PORTFOLIO}) {
            assertTrue(mThemeStore.getWpComMobileFriendlyThemes(category).size() > 0);
        }
    }

    @Test
    public void testActivateTheme() throws InterruptedException {
        // Make sure no theme is active at first
        assertNull(mThemeStore.getActiveThemeForSite(sSite));
        ThemeModel currentTheme = fetchCurrentTheme();
        assertNotNull(currentTheme);

        // Our test site is on a business plan, so it's considered a Jetpack site. Some time ago Jetpack sites were
        // only self-hosted sites and had only access to themes installed in the site. So, even though at the time of
        // this comment it's possible to install wp.com themes, ThemeStore is not yet updated to work that way.
        // It seems WPAndroid is getting around this restriction by using a browser for browsing and activating themes.
        //
        // In this test we are using the installed themes as if we can't access the wp.com themes to match the current
        // ThemeStore behavior.
        fetchSiteThemes();

        // activate a different "Twenty ..." theme
        ThemeModel themeToActivate = getTwentySomethingFreeTheme(currentTheme.getThemeId(),
                mThemeStore.getThemesForSite(sSite));
        assertNotNull(themeToActivate);
        activateTheme(themeToActivate);

        // Assert that the activation was successful
        ThemeModel activatedTheme = mThemeStore.getActiveThemeForSite(sSite);
        assertNotNull(activatedTheme);
        assertEquals(activatedTheme.getThemeId(), themeToActivate.getThemeId());
    }

    @Test
    public void testFetchStarterDesigns() throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_STARTER_DESIGNS;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchStarterDesignsAction(
                new ThemeStore.FetchStarterDesignsPayload(320.0f, 480.0f, 1f)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchStarterDesignsWithGroups() throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_STARTER_DESIGNS;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchStarterDesignsAction(
                new ThemeStore.FetchStarterDesignsPayload(320.0f, 480.0f, 1f, "single-page")));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemeActivated(OnThemeActivated event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.ACTIVATED_THEME, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onWpComThemesChanged(OnWpComThemesChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.FETCHED_WPCOM_THEMES, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteThemesChanged(OnSiteThemesChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.FETCHED_SITE_THEMES, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentThemeFetched(OnCurrentThemeFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }

        assertEquals(TestEvents.FETCHED_CURRENT_THEME, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onStarterDesignsFetched(OnStarterDesignsFetched event) {
        assertEquals(mNextEvent, TestEvents.FETCHED_STARTER_DESIGNS);
        assertFalse(event.isError());
        assertNotNull(event.designs);
        assertFalse(event.designs.isEmpty());
        assertFalse(event.categories.isEmpty());
        mCountDownLatch.countDown();
    }

    private ThemeModel getTwentySomethingFreeTheme(String oldThemeId, List<ThemeModel> themes) {
        for (ThemeModel theme : themes) {
            if (!theme.getThemeId().equals(oldThemeId) && theme.getFree()) {
                return theme;
            }
        }
        return null;
    }

    private ThemeModel fetchCurrentTheme() throws InterruptedException {
        // fetch current theme
        mNextEvent = TestEvents.FETCHED_CURRENT_THEME;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(sSite));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        return mThemeStore.getActiveThemeForSite(sSite);
    }

    private void fetchWpComThemes() throws InterruptedException {
        // no need to sign into account, this is a test for an endpoint that does not require authentication
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_WPCOM_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction(new FetchWPComThemesPayload()));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSiteThemes() throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_SITE_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchInstalledThemesAction(sSite));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void activateTheme(ThemeModel themeToActivate) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.ACTIVATED_THEME;
        SiteThemePayload payload = new SiteThemePayload(sSite, themeToActivate);
        mDispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
