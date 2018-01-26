package org.wordpress.android.fluxc.release;

import junit.framework.Assert;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_ThemeTestWPCom extends ReleaseStack_WPComBase {
    enum TestEvents {
        NONE,
        FETCHED_WPCOM_THEMES,
        FETCHED_CURRENT_THEME,
        ACTIVATED_THEME
    }

    @Inject ThemeStore mThemeStore;

    private TestEvents mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
    }

    public void testFetchCurrentTheme() throws InterruptedException {
        // Make sure no theme is active at first
        assertNull(mThemeStore.getActiveThemeForSite(sSite));
        ThemeModel currentTheme = fetchCurrentTheme();
        Assert.assertNotNull(currentTheme);
    }

    public void testFetchWPComThemes() throws InterruptedException {
        // verify themes don't already exist in store
        Assert.assertTrue(mThemeStore.getWpComThemes().isEmpty());
        fetchWpComThemes();
        // verify response received and WP themes list is not empty
        Assert.assertFalse(mThemeStore.getWpComThemes().isEmpty());

        // verify that we have the 3 mobile-friendly categories being non empty
        for (String category : new String[]{ThemeStore.MOBILE_FRIENDLY_CATEGORY_BLOG,
                ThemeStore.MOBILE_FRIENDLY_CATEGORY_WEBSITE, ThemeStore.MOBILE_FRIENDLY_CATEGORY_PORTFOLIO}) {
            Assert.assertTrue(mThemeStore.getWpComMobileFriendlyThemes(category).size() > 0);
        }
    }

    public void testActivateTheme() throws InterruptedException {
        // Make sure no theme is active at first
        assertNull(mThemeStore.getActiveThemeForSite(sSite));
        ThemeModel currentTheme = fetchCurrentTheme();
        Assert.assertNotNull(currentTheme);

        // Fetch wp.com themes to activate a different theme
        fetchWpComThemes();

        // activate a different theme
        ThemeModel themeToActivate = getNewNonPremiumTheme(currentTheme.getThemeId(), mThemeStore.getWpComThemes());
        Assert.assertNotNull(themeToActivate);
        activateTheme(themeToActivate);

        // Assert that the activation was successful
        ThemeModel activatedTheme = mThemeStore.getActiveThemeForSite(sSite);
        Assert.assertNotNull(activatedTheme);
        Assert.assertEquals(activatedTheme.getThemeId(), themeToActivate.getThemeId());
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemeActivated(ThemeStore.OnThemeActivated event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        Assert.assertTrue(mNextEvent == TestEvents.ACTIVATED_THEME);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onWpComThemesChanged(ThemeStore.OnWpComThemesChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        Assert.assertEquals(mNextEvent, TestEvents.FETCHED_WPCOM_THEMES);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentThemeFetched(ThemeStore.OnCurrentThemeFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }

        Assert.assertTrue(mNextEvent == TestEvents.FETCHED_CURRENT_THEME);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(AccountStore.OnAccountChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    private ThemeModel getNewNonPremiumTheme(String oldThemeId, List<ThemeModel> themes) {
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
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        return mThemeStore.getActiveThemeForSite(sSite);
    }

    private void fetchWpComThemes() throws InterruptedException {
        // no need to sign into account, this is a test for an endpoint that does not require authentication
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_WPCOM_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction());
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void activateTheme(ThemeModel themeToActivate) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.ACTIVATED_THEME;
        SiteThemePayload payload = new SiteThemePayload(sSite, themeToActivate);
        mDispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
