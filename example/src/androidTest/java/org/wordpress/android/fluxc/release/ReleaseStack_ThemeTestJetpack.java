package org.wordpress.android.fluxc.release;

import android.support.annotation.NonNull;

import junit.framework.Assert;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.ThemeAction;
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
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_ThemeTestJetpack extends ReleaseStack_Base {
    enum TestEvents {
        NONE,
        FETCHED_WPCOM_THEMES,
        FETCHED_INSTALLED_THEMES,
        FETCHED_CURRENT_THEME,
        ACTIVATED_THEME,
        INSTALLED_THEME,
        DELETED_THEME,
        REMOVED_THEME,
        REMOVED_SITE_THEMES,
        SITE_CHANGED,
        SITE_REMOVED
    }

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ThemeStore mThemeStore;

    private TestEvents mNextEvent;

    private static final String EDIN_THEME_ID = "edin-wpcom";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
    }

    public void testFetchInstalledThemes() throws InterruptedException {
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();

        // verify that installed themes list is empty first
        Assert.assertTrue(mThemeStore.getThemesForSite(jetpackSite).size() == 0);

        // fetch installed themes
        fetchInstalledThemes(jetpackSite);

        // verify themes are available for the site
        Assert.assertTrue(mThemeStore.getThemesForSite(jetpackSite).size() > 0);

        signOutWPCom();
    }

    public void testFetchCurrentTheme() throws InterruptedException {
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();

        // Verify there is no active theme for the site at the start
        assertNull(mThemeStore.getActiveThemeForSite(jetpackSite));

        // fetch active theme
        ThemeModel currentTheme = fetchCurrentTheme(jetpackSite);
        Assert.assertNotNull(currentTheme);

        signOutWPCom();
    }

    public void testActivateTheme() throws InterruptedException {
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();

        // fetch installed themes
        fetchInstalledThemes(jetpackSite);

        // make sure there are at least 2 themes, one that's active and one that will be activated
        List<ThemeModel> themes = mThemeStore.getThemesForSite(jetpackSite);
        Assert.assertTrue(themes.size() > 1);

        // fetch active theme
        ThemeModel currentTheme = fetchCurrentTheme(jetpackSite);
        Assert.assertNotNull(currentTheme);

        // select a different theme to activate
        ThemeModel themeToActivate = getOtherTheme(themes, currentTheme.getThemeId());
        Assert.assertNotNull(themeToActivate);

        // activate it
        ThemeModel activatedTheme = activateTheme(jetpackSite, themeToActivate);
        Assert.assertNotNull(activatedTheme);
        Assert.assertEquals(activatedTheme.getThemeId(), themeToActivate.getThemeId());

        signOutWPCom();
    }

    public void testInstallTheme() throws InterruptedException {
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();
        final String themeId = EDIN_THEME_ID;

        // fetch installed themes
        fetchInstalledThemes(jetpackSite);

        // make sure there are at least 2 themes, one that's active and one that may be activated
        List<ThemeModel> themes = mThemeStore.getThemesForSite(jetpackSite);
        Assert.assertTrue(themes.size() > 1);

        // If the theme is already installed, delete it first
        ThemeModel installedTheme = mThemeStore.getInstalledThemeByThemeId(jetpackSite, themeId);
        if (installedTheme != null) {
            deactivateAndDeleteTheme(jetpackSite, installedTheme);
        }

        // install the theme
        ThemeModel themeToInstall = new ThemeModel();
        themeToInstall.setThemeId(themeId);
        installTheme(jetpackSite, themeToInstall);
        Assert.assertTrue(isThemeInstalled(jetpackSite, themeId));

        signOutWPCom();
    }

    public void testDeleteTheme() throws InterruptedException {
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();
        final String themeId = EDIN_THEME_ID;

        // fetch installed themes
        fetchInstalledThemes(jetpackSite);

        // make sure there are at least 2 themes, one that's active and one that may be activated
        List<ThemeModel> themes = mThemeStore.getThemesForSite(jetpackSite);
        Assert.assertTrue(themes.size() > 1);

        // Install edin if necessary before attempting to delete
        if (!isThemeInstalled(jetpackSite, themeId)) {
            ThemeModel themeToInstall = new ThemeModel();
            themeToInstall.setThemeId(themeId);
            installTheme(jetpackSite, themeToInstall);

            // make sure theme is available for site (install was successful)
            Assert.assertTrue(isThemeInstalled(jetpackSite, themeId));
        }

        // Get the theme from store to make sure the "active" state is correct, so we can deactivate it before deletion
        ThemeModel themeToDelete = mThemeStore.getInstalledThemeByThemeId(jetpackSite, EDIN_THEME_ID);
        // if Edin is active update site's active theme to something else and delete Edin
        deactivateAndDeleteTheme(jetpackSite, themeToDelete);

        signOutWPCom();
    }

    public void testRemoveSiteThemes() throws InterruptedException {
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();

        // verify initial state, no themes in store
        Assert.assertTrue(mThemeStore.getThemesForSite(jetpackSite).isEmpty());
        Assert.assertTrue(mThemeStore.getWpComThemes().isEmpty());

        // fetch themes for site and WP.com themes
        fetchInstalledThemes(jetpackSite);
        fetchWpComThemes();

        // Verify fetches were successful
        Assert.assertFalse(mThemeStore.getThemesForSite(jetpackSite).isEmpty());
        Assert.assertFalse(mThemeStore.getWpComThemes().isEmpty());

        // remove the site's themes
        removeSiteThemes(jetpackSite);

        // verify site themes are removed and that WP.com themes are still there
        Assert.assertTrue(mThemeStore.getThemesForSite(jetpackSite).isEmpty());
        Assert.assertFalse(mThemeStore.getWpComThemes().isEmpty());

        // sign out
        signOutWPCom();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteThemesChanged(ThemeStore.OnSiteThemesChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        if (event.origin == ThemeAction.FETCH_INSTALLED_THEMES) {
            Assert.assertEquals(mNextEvent, TestEvents.FETCHED_INSTALLED_THEMES);
            mCountDownLatch.countDown();
        } else if (event.origin == ThemeAction.REMOVE_SITE_THEMES) {
            Assert.assertEquals(mNextEvent, TestEvents.REMOVED_SITE_THEMES);
            mCountDownLatch.countDown();
        } else {
            throw new AssertionError("Unexpected event occurred from origin: " + event.origin);
        }
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
        Assert.assertNotNull(event.theme);
        mCountDownLatch.countDown();
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
    public void onThemeInstalled(ThemeStore.OnThemeInstalled event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        Assert.assertTrue(mNextEvent == TestEvents.INSTALLED_THEME);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemeDeleted(ThemeStore.OnThemeDeleted event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        Assert.assertTrue(mNextEvent == TestEvents.DELETED_THEME);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemeRemoved(ThemeStore.OnThemeRemoved event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        Assert.assertEquals(TestEvents.REMOVED_THEME, mNextEvent);
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
        Assert.assertEquals(TestEvents.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    private SiteModel signIntoWpComAccountWithJetpackSite() throws InterruptedException {
        // sign into a WP.com account with a Jetpack site
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);

        // verify Jetpack site is available
        final SiteModel jetpackSite = getJetpackSite();
        Assert.assertNotNull(jetpackSite);
        return jetpackSite;
    }

    private void authenticateWPComAndFetchSites(String username, String password) throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AccountStore.AuthenticatePayload payload = new AccountStore.AuthenticatePayload(username, password);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        // Wait for a network response / onChanged event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch account from REST API, and wait for OnAccountChanged event
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        Assert.assertTrue(mSiteStore.getSitesCount() > 0);
    }

    private void signOutWPCom() throws InterruptedException {
        // Clear WP.com sites, and wait for OnSiteRemoved event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_REMOVED;
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction());
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchWpComThemes() throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_WPCOM_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction());
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchInstalledThemes(@NonNull SiteModel jetpackSite) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_INSTALLED_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchInstalledThemesAction(jetpackSite));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private ThemeModel fetchCurrentTheme(@NonNull SiteModel jetpackSite) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_CURRENT_THEME;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(jetpackSite));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        return mThemeStore.getActiveThemeForSite(jetpackSite);
    }

    private ThemeModel activateTheme(@NonNull SiteModel jetpackSite, @NonNull ThemeModel themeToActivate)
            throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.ACTIVATED_THEME;
        SiteThemePayload payload = new SiteThemePayload(jetpackSite, themeToActivate);
        mDispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(payload));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        return mThemeStore.getActiveThemeForSite(jetpackSite);
    }

    private void deactivateAndDeleteTheme(@NonNull SiteModel jetpackSite, @NonNull ThemeModel theme)
            throws InterruptedException {
        // An active theme can't be deleted, first activate a different theme
        if (theme.getActive()) {
            ThemeModel otherThemeToActivate = getOtherTheme(mThemeStore.getThemesForSite(jetpackSite),
                    EDIN_THEME_ID);
            Assert.assertNotNull(otherThemeToActivate);
            activateTheme(jetpackSite, otherThemeToActivate);

            // Make sure another theme is activated
            Assert.assertFalse(theme.getThemeId().equals(mThemeStore.getActiveThemeForSite(jetpackSite).getThemeId()));
        }
        // delete existing theme from site
        deleteTheme(jetpackSite, theme);
        // make sure theme is no longer available for site (delete was successful)
        Assert.assertFalse(isThemeInstalled(jetpackSite, theme.getThemeId()));
    }

    private void removeSiteThemes(@NonNull SiteModel site) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.REMOVED_SITE_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newRemoveSiteThemesAction(site));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void installTheme(@NonNull SiteModel site, @NonNull ThemeModel theme) throws InterruptedException {
        SiteThemePayload install = new SiteThemePayload(site, theme);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.INSTALLED_THEME;
        mDispatcher.dispatch(ThemeActionBuilder.newInstallThemeAction(install));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void deleteTheme(@NonNull SiteModel site, @NonNull ThemeModel theme) throws InterruptedException {
        SiteThemePayload delete = new SiteThemePayload(site, theme);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.DELETED_THEME;
        mDispatcher.dispatch(ThemeActionBuilder.newDeleteThemeAction(delete));
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private SiteModel getJetpackSite() {
        for (SiteModel site : mSiteStore.getSites()) {
            if (site.isJetpackConnected()) {
                return site;
            }
        }
        return null;
    }

    private ThemeModel getOtherTheme(@NonNull List<ThemeModel> themes, @NonNull String idToIgnore) {
        for (ThemeModel theme : themes) {
            if (!idToIgnore.equals(theme.getThemeId())) {
                return theme;
            }
        }
        return null;
    }

    // Make sure to fetch installed themes before calling this
    private boolean isThemeInstalled(@NonNull SiteModel jetpackSite, @NonNull String themeId) {
        return mThemeStore.getInstalledThemeByThemeId(jetpackSite, themeId) != null;
    }
}
