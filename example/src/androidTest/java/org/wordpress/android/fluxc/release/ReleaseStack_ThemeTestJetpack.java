package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.test.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.ThemeStore;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_ThemeTestJetpack extends ReleaseStack_Base {
    enum TestEvents {
        NONE,
        FETCHED_INSTALLED_THEMES,
        FETCHED_CURRENT_THEME,
        ACTIVATED_THEME,
        SITE_CHANGED,
        SITE_REMOVED
    }

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ThemeStore mThemeStore;
    private ThemeModel mCurrentTheme;
    private ThemeModel mActivatedTheme;

    private TestEvents mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
        mCurrentTheme = null;
        mActivatedTheme = null;
    }

    public void testFetchInstalledThemes() throws InterruptedException {
        // sign into a WP.com account with a Jetpack site
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);

        // verify Jetpack site is available
        SiteModel jetpackSite = getJetpackSite();
        assertNotNull(jetpackSite);

        // fetch installed themes
        mNextEvent = TestEvents.FETCHED_INSTALLED_THEMES;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(ThemeActionBuilder.newFetchInstalledThemesAction(jetpackSite));

        // verify response received and themes are available for the site
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mThemeStore.getThemesForSite(jetpackSite).size() > 0);

        signOutWPCom();
    }

    public void testFetchCurrentTheme() throws InterruptedException {
        // sign into a WP.com account with a Jetpack site
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);

        // verify Jetpack site is available
        SiteModel jetpackSite = getJetpackSite();
        assertNotNull(jetpackSite);

        // fetch current theme
        mNextEvent = TestEvents.FETCHED_CURRENT_THEME;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(jetpackSite));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        signOutWPCom();
    }

    public void testActivateTheme() throws InterruptedException {
        // get current active theme on a site
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);
        SiteModel jetpackSite = getJetpackSite();
        assertNotNull(jetpackSite);

        // get all themes available
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_INSTALLED_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchInstalledThemesAction(jetpackSite));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        List<ThemeModel> themes = mThemeStore.getThemesForSite(jetpackSite);
        assertTrue(themes.size() > 1);

        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_CURRENT_THEME;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(jetpackSite));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotNull(mCurrentTheme);

        // activate a different theme
        ThemeModel themeToActivate = mCurrentTheme.getThemeId().equals(themes.get(0).getThemeId())
                ? themes.get(1) : themes.get(0);
        assertNotNull(themeToActivate);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.ACTIVATED_THEME;
        ThemeStore.ActivateThemePayload payload = new ThemeStore.ActivateThemePayload(jetpackSite, themeToActivate);
        mDispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotNull(mActivatedTheme);
        assertEquals(mActivatedTheme.getThemeId(), themeToActivate.getThemeId());

        signOutWPCom();
    }

    public void testInstallTheme() throws InterruptedException {
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemesChanged(ThemeStore.OnThemesChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }

        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentThemeFetched(ThemeStore.OnCurrentThemeFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }

        if (mNextEvent == TestEvents.FETCHED_CURRENT_THEME) {
            assertNotNull(event.theme);
            mCurrentTheme = event.theme;
            mCountDownLatch.countDown();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemeActivated(ThemeStore.OnThemeActivated event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertTrue(mNextEvent == TestEvents.ACTIVATED_THEME);
        mActivatedTheme = event.theme;
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

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteRemoved(SiteStore.OnSiteRemoved event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    private void authenticateWPComAndFetchSites(String username, String password) throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AccountStore.AuthenticatePayload payload = new AccountStore.AuthenticatePayload(username, password);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch account from REST API, and wait for OnAccountChanged event
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSiteStore.getSitesCount() > 0);
    }

    private void signOutWPCom() throws InterruptedException {
        // Clear WP.com sites, and wait for OnSiteRemoved event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_REMOVED;
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private SiteModel getJetpackSite() {
        for (SiteModel site : mSiteStore.getSites()) {
            if (site.isJetpackConnected()) {
                return site;
            }
        }
        return null;
    }
}
