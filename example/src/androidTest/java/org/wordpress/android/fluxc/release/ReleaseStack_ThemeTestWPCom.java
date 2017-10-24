package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.fluxc.store.ThemeStore.ActivateThemePayload;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_ThemeTestWPCom extends ReleaseStack_WPComBase {
    enum TestEvents {
        NONE,
        FETCHED_THEMES,
        FETCHED_CURRENT_THEME,
        SEARCHED_THEMES,
        ACTIVATED_THEME,
    }

    @Inject ThemeStore mThemeStore;

    private TestEvents mNextEvent;
    private ThemeModel mCurrentTheme;
    private ThemeModel mActivatedTheme;
    private List<ThemeModel> mSearchResults;

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
        mSearchResults = null;
    }

    public void testActivateTheme() throws InterruptedException {
        // get all themes available
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // get current active theme on a site
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_CURRENT_THEME;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(sSite));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotNull(mCurrentTheme);

        // activate a different theme
        ThemeModel themeToActivate = getNewNonPremiumTheme(mCurrentTheme.getThemeId(), mThemeStore.getWpComThemes());
        assertNotNull(themeToActivate);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.ACTIVATED_THEME;
        ActivateThemePayload payload = new ActivateThemePayload(sSite, themeToActivate);
        mDispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotNull(mActivatedTheme);
        assertEquals(mActivatedTheme.getThemeId(), themeToActivate.getThemeId());
    }

    public void testFetchWPComThemes() throws InterruptedException {
        // verify themes don't already exist in store
        assertTrue(mThemeStore.getWpComThemes().isEmpty());

        // no need to sign into account, this is a test for an endpoint that does not require authentication
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction());

        // verify response received and WP themes list is not empty
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mThemeStore.getWpComThemes().isEmpty());
    }

    public void testFetchCurrentTheme() throws InterruptedException {
        // fetch current theme
        mNextEvent = TestEvents.FETCHED_CURRENT_THEME;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(sSite));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotNull(mCurrentTheme);
    }

    public void testSearchThemes() throws InterruptedException {
        // "Twenty *teen" themes
        final String searchTerm = "twenty";

        ThemeStore.SearchThemesPayload payload = new ThemeStore.SearchThemesPayload(searchTerm);
        mNextEvent = TestEvents.SEARCHED_THEMES;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(ThemeActionBuilder.newSearchThemesAction(payload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotNull(mSearchResults);
        assertFalse(mSearchResults.isEmpty());
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemesSearched(ThemeStore.OnThemesSearched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertTrue(mNextEvent == TestEvents.SEARCHED_THEMES);
        mSearchResults = event.searchResults;
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemeActivated(ThemeStore.OnThemeActivated event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertTrue(mNextEvent == TestEvents.ACTIVATED_THEME);
        mActivatedTheme = event.theme;
        mCurrentTheme = event.theme;
        mCountDownLatch.countDown();
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

        assertTrue(mNextEvent == TestEvents.FETCHED_CURRENT_THEME);
        assertNotNull(event.theme);
        mCurrentTheme = event.theme;
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
            if (!theme.getThemeId().equals(oldThemeId) && theme.getPrice() == 0.f) {
                return theme;
            }
        }
        return null;
    }
}
