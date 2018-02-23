package org.wordpress.android.fluxc.release;

import junit.framework.Assert;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.fluxc.store.PluginStore.FetchPluginDirectoryPayload;
import org.wordpress.android.fluxc.store.PluginStore.OnPluginDirectoryFetched;
import org.wordpress.android.fluxc.store.PluginStore.OnPluginDirectorySearched;
import org.wordpress.android.fluxc.store.PluginStore.OnWPOrgPluginFetched;
import org.wordpress.android.fluxc.store.PluginStore.SearchPluginDirectoryPayload;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ReleaseStack_WPOrgPluginTest extends ReleaseStack_Base {
    @Inject PluginStore mPluginStore;

    enum TestEvents {
        NONE,
        PLUGIN_DIRECTORY_FETCHED,
        PLUGIN_DIRECTORY_SEARCHED,
        WPORG_PLUGIN_FETCHED,
        WPORG_PLUGIN_DOES_NOT_EXIST
    }

    private TestEvents mNextEvent;
    private int mSearchPage;

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
    public void testFetchWPOrgPlugin() throws InterruptedException {
        String slug = "akismet";
        mNextEvent = TestEvents.WPORG_PLUGIN_FETCHED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(slug));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        WPOrgPluginModel wpOrgPlugin = mPluginStore.getWPOrgPluginBySlug(slug);
        assertNotNull(wpOrgPlugin);
    }

    // This is a long set of tests that makes sure the pagination works correctly
    public void testFetchPluginDirectory() throws InterruptedException {
        PluginDirectoryType primaryType = PluginDirectoryType.NEW;
        Assert.assertTrue(mPluginStore.getPluginDirectory(primaryType).size() == 0);

        fetchPluginDirectory(primaryType, false);

        List<WPOrgPluginModel> firstPluginList = mPluginStore.getPluginDirectory(primaryType);
        Assert.assertTrue(firstPluginList.size() > 0);

        // Do another fetch this time loading the second page
        fetchPluginDirectory(primaryType, true);

        // Assert that new items are fetched
        List<WPOrgPluginModel> secondPluginList = mPluginStore.getPluginDirectory(primaryType);
        Assert.assertTrue(secondPluginList.size() > firstPluginList.size());

        // Do one more fetch this time a different directory type and make sure it didn't affect the primary one
        PluginDirectoryType secondaryType = PluginDirectoryType.POPULAR;
        Assert.assertTrue(mPluginStore.getPluginDirectory(secondaryType).size() == 0);

        fetchPluginDirectory(secondaryType, false);

        // Assert no new items fetched for primary type, but instead they are fetched for the secondary type
        List<WPOrgPluginModel> thirdPluginList = mPluginStore.getPluginDirectory(primaryType);
        Assert.assertTrue(thirdPluginList.size() == secondPluginList.size());
        Assert.assertTrue(mPluginStore.getPluginDirectory(secondaryType).size() > 0);

        // Do one more FRESH fetch to make sure that previous items are deleted
        fetchPluginDirectory(primaryType, false);

        // Assert the number of items is the same as the first fetch
        List<WPOrgPluginModel> fourthPluginList = mPluginStore.getPluginDirectory(primaryType);
        Assert.assertTrue(firstPluginList.size() == fourthPluginList.size());
    }

    // This simulates the pull to refresh feature a client might implement
    public void testFetchSamePageOfPluginDirectory() throws InterruptedException {
        PluginDirectoryType directoryType = PluginDirectoryType.NEW;
        Assert.assertTrue(mPluginStore.getPluginDirectory(directoryType).size() == 0);

        // Fetch plugin directory's first page
        fetchPluginDirectory(directoryType, false);

        List<WPOrgPluginModel> pluginsAfterFirstFetch = mPluginStore.getPluginDirectory(directoryType);
        Assert.assertTrue(pluginsAfterFirstFetch.size() > 0);

        // Re-fetch plugin directory's first page
        fetchPluginDirectory(directoryType, false);

        // Same number of items should have been fetched and the existing plugin directories should be updated
        List<WPOrgPluginModel> pluginsAfterSecondFetch = mPluginStore.getPluginDirectory(directoryType);
        Assert.assertEquals(pluginsAfterFirstFetch.size(), pluginsAfterSecondFetch.size());
    }

    public void testFetchWPOrgPluginDoesNotExistError() throws InterruptedException {
        String slug = "hello";
        mNextEvent = TestEvents.WPORG_PLUGIN_DOES_NOT_EXIST;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(slug));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSearchPluginDirectory() throws InterruptedException {
        // This search term is picked because it has more than 100 results to test pagination
        String searchTerm = "Writing";

        // Search first page
        searchPluginDirectory(searchTerm, 1);

        // Search second page
        searchPluginDirectory(searchTerm, 2);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPluginDirectoryFetched(OnPluginDirectoryFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.PLUGIN_DIRECTORY_FETCHED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPluginDirectorySearched(OnPluginDirectorySearched event) throws InterruptedException {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.PLUGIN_DIRECTORY_SEARCHED, mNextEvent);
        // Assert that we got some plugins
        assertTrue(event.plugins != null && event.plugins.size() > 0);
        assertEquals(mSearchPage, event.page);
        assertNotNull(event.searchTerm);
        assertTrue(event.canLoadMore);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onWPOrgPluginFetched(OnWPOrgPluginFetched event) {
        if (event.isError()) {
            assertEquals(mNextEvent, TestEvents.WPORG_PLUGIN_DOES_NOT_EXIST);
            mCountDownLatch.countDown();
            return;
        }

        assertEquals(TestEvents.WPORG_PLUGIN_FETCHED, mNextEvent);
        mCountDownLatch.countDown();
    }

    // Network helpers

    private void fetchPluginDirectory(PluginDirectoryType directoryType, boolean loadMore) throws InterruptedException {
        mNextEvent = TestEvents.PLUGIN_DIRECTORY_FETCHED;
        mCountDownLatch = new CountDownLatch(1);
        FetchPluginDirectoryPayload payload = new FetchPluginDirectoryPayload(directoryType, loadMore);
        mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void searchPluginDirectory(String searchTerm, int page) throws InterruptedException {
        mSearchPage = page;
        mNextEvent = TestEvents.PLUGIN_DIRECTORY_SEARCHED;
        mCountDownLatch = new CountDownLatch(1);
        SearchPluginDirectoryPayload payload = new SearchPluginDirectoryPayload(searchTerm, page);
        mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
