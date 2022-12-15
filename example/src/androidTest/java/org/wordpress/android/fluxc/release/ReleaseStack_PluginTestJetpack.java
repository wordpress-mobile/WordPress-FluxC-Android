package org.wordpress.android.fluxc.release;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginErrorType;
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginErrorType;
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchPluginDirectoryPayload;
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginErrorType;
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.OnPluginDirectoryFetched;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginDeleted;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginUpdated;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginsRemoved;
import org.wordpress.android.fluxc.store.PluginStore.PluginDirectoryErrorType;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginErrorType;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReleaseStack_PluginTestJetpack extends ReleaseStack_Base {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject PluginStore mPluginStore;

    private static final String JETPACK_PLUGIN_NAME = "jetpack/jetpack";

    enum TestEvents {
        NONE,
        DELETE_SITE_PLUGIN_ERROR,
        DELETED_SITE_PLUGIN,
        INSTALLED_SITE_PLUGIN,
        INSTALL_SITE_PLUGIN_ERROR_NO_PACKAGE,
        SITE_PLUGINS_FETCHED,
        SITE_CHANGED,
        SITE_REMOVED,
        UNKNOWN_SITE_PLUGIN,
        CONFIGURED_SITE_PLUGIN,
        REMOVED_SITE_PLUGINS,
        PLUGIN_ACTION_NOT_AVAILABLE
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
    public void testFetchSitePlugins() throws InterruptedException {
        SiteModel site = fetchSingleJetpackSitePlugins();

        List<ImmutablePluginModel> plugins = mPluginStore.getPluginDirectory(site, PluginDirectoryType.SITE);
        assertTrue(plugins.size() > 0);

        signOutWPCom();
    }

    @Test
    public void testConfigureSitePlugin() throws InterruptedException {
        // In order to have a reliable test, let's first fetch the list of plugins, pick the first plugin
        // and change it's active status, so we can make sure when we run the test multiple times, each time
        // an action is actually taken. This wouldn't be the case if we always activate the plugin.
        SiteModel site = fetchSingleJetpackSitePlugins();

        List<ImmutablePluginModel> plugins = mPluginStore.getPluginDirectory(site, PluginDirectoryType.SITE);
        ImmutablePluginModel immutablePlugin = null;
        // Find first plugin that's not Jetpack since we can't deactivate Jetpack
        for (ImmutablePluginModel pl : plugins) {
            if (!JETPACK_PLUGIN_NAME.equalsIgnoreCase(pl.getName())) {
                immutablePlugin = pl;
                break;
            }
        }
        assertNotNull(immutablePlugin);
        assertTrue(immutablePlugin.isInstalled());
        boolean isActive = !immutablePlugin.isActive();

        ConfigureSitePluginPayload payload = new ConfigureSitePluginPayload(site, immutablePlugin.getName(),
                immutablePlugin.getSlug(), isActive, immutablePlugin.isAutoUpdateEnabled());
        configureSitePlugin(payload, TestEvents.CONFIGURED_SITE_PLUGIN);

        ImmutablePluginModel configuredPlugin = mPluginStore.getImmutablePluginBySlug(site, immutablePlugin.getSlug());
        assertNotNull(configuredPlugin);
        assertTrue(configuredPlugin.isInstalled());
        assertEquals(configuredPlugin.isActive(), isActive);

        signOutWPCom();
    }

    // It's both easier and more efficient to combine install & delete tests since we need to make sure we have the
    // plugin installed for the delete test and the plugin is not installed for the install test
    @Test
    public void testInstallAndDeleteSitePlugin() throws InterruptedException {
        String prefix = "ManualDebugging";
        String pluginSlugToInstall = "react";
        // Fetch the list of installed plugins to make sure `React` is not installed
        SiteModel site = fetchSingleJetpackSitePlugins();

        Log.i(prefix, "Getting Plugins List");
        List<ImmutablePluginModel> plugins = mPluginStore.getPluginDirectory(site, PluginDirectoryType.SITE);
        Log.i(prefix, "Iterating through plugins");
        for (ImmutablePluginModel immutablePlugin : plugins) {
            Log.i(prefix, "Assert if current plugin is installed");
            assertTrue(immutablePlugin.isInstalled());

            Log.i(prefix, "Check if current plugin is 'react'");
            if (pluginSlugToInstall.equals(immutablePlugin.getSlug())) {
                // We need to deactivate the plugin to be able to uninstall it
                Log.i(prefix, "Current plugin is 'react'. Check if it's active");
                if (immutablePlugin.isActive()) {
                    Log.i(prefix, "Trying to deactivate current plugin");
                    deactivatePlugin(site, immutablePlugin);
                }

                // delete plugin first
                Log.i(prefix, "Trying to delete current plugin");
                deleteSitePlugin(site, immutablePlugin);
            }
        }

        // Install the React plugin
        Log.i(prefix, "Trying to install react");
        installSitePlugin(site, pluginSlugToInstall);

        Log.i(prefix, "Trying to get the installed react plugin by name");
        ImmutablePluginModel installedPlugin = mPluginStore.getImmutablePluginBySlug(site,
                pluginSlugToInstall);
        Log.i(prefix, "assert not null");
        assertNotNull(installedPlugin);
        Log.i(prefix, "assert installed");
        assertTrue(installedPlugin.isInstalled());

        // We need to deactivate the plugin to be able to uninstall it
        Log.i(prefix, "check if active");
        if (installedPlugin.isActive()) {
            Log.i(prefix, "deactivate");
            deactivatePlugin(site, installedPlugin);
        }

        // Delete the newly installed React plugin
        Log.i(prefix, "delete");
        deleteSitePlugin(site, installedPlugin);

        Log.i(prefix, "Getting Plugins List once again");
        List<ImmutablePluginModel> updatedPlugins = mPluginStore.getPluginDirectory(site, PluginDirectoryType.SITE);
        Log.i(prefix, "iterate through Plugins List once again");
        for (ImmutablePluginModel immutablePlugin : updatedPlugins) {
            Log.i(prefix, "make sure current plugin is not react");
            assertNotEquals(pluginSlugToInstall, immutablePlugin.getSlug());
        }

        Log.i(prefix, "sign out");
        signOutWPCom();
    }

    @Test
    public void testConfigureUnknownPluginError() throws InterruptedException {
        SiteModel site = authenticateAndRetrieveSingleJetpackSite();
        String pluginName = "this-plugin-does-not-exist-name";
        String pluginSlug = "this-plugin-does-not-exist-slug";

        ConfigureSitePluginPayload payload = new ConfigureSitePluginPayload(site, pluginName, pluginSlug, false, false);
        configureSitePlugin(payload, TestEvents.UNKNOWN_SITE_PLUGIN);

        signOutWPCom();
    }

    @Test
    public void testDeleteActivePluginError() throws InterruptedException {
        SiteModel site = fetchSingleJetpackSitePlugins();

        ImmutablePluginModel activePluginToTest = null;

        List<ImmutablePluginModel> immutablePlugins = mPluginStore.getPluginDirectory(site, PluginDirectoryType.SITE);
        for (ImmutablePluginModel immutablePlugin : immutablePlugins) {
            assertTrue(immutablePlugin.isInstalled());
            if (immutablePlugin.isActive()) {
                activePluginToTest = immutablePlugin;
                break;
            }
        }

        assertNotNull(activePluginToTest);

        // Trying to delete an active plugin should result in DELETE_SITE_PLUGIN_ERROR
        deleteSitePlugin(site, activePluginToTest, TestEvents.DELETE_SITE_PLUGIN_ERROR);

        signOutWPCom();
    }

    // Trying to remove a plugin that doesn't exist in remote should remove the plugin from DB
    @Test
    public void testDeleteUnknownPlugin() throws InterruptedException {
        SiteModel site = fetchSingleJetpackSitePlugins();

        String pluginName = "this-plugin-does-not-exist-name";
        String pluginSlug = "this-plugin-does-not-exist-slug";
        SitePluginModel plugin = new SitePluginModel();
        plugin.setName(pluginName);
        plugin.setSlug(pluginSlug);
        plugin.setLocalSiteId(site.getId());
        PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin);

        ImmutablePluginModel immutablePlugin = mPluginStore.getImmutablePluginBySlug(site, pluginSlug);
        assertNotNull(immutablePlugin);
        assertTrue(immutablePlugin.isInstalled());

        mNextEvent = TestEvents.DELETED_SITE_PLUGIN;
        mCountDownLatch = new CountDownLatch(1);

        DeleteSitePluginPayload payload = new DeleteSitePluginPayload(site, immutablePlugin.getName(),
                immutablePlugin.getSlug());
        mDispatcher.dispatch(PluginActionBuilder.newDeleteSitePluginAction(payload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure the plugin is removed from DB
        assertNull(mPluginStore.getImmutablePluginBySlug(site, pluginSlug));

        signOutWPCom();
    }

    @Test
    public void testInstallPluginNoPackageError() throws InterruptedException {
        String slug = "this-plugin-does-not-exist";
        SiteModel site = fetchSingleJetpackSitePlugins();
        installSitePlugin(site, slug, TestEvents.INSTALL_SITE_PLUGIN_ERROR_NO_PACKAGE);
        ImmutablePluginModel immutablePlugin = mPluginStore.getImmutablePluginBySlug(site, slug);
        assertNull(immutablePlugin);

        signOutWPCom();
    }

    @Test
    public void testRemoveSitePlugins() throws InterruptedException {
        SiteModel site = fetchSingleJetpackSitePlugins();
        List<ImmutablePluginModel> plugins = mPluginStore.getPluginDirectory(site, PluginDirectoryType.SITE);
        assertTrue(plugins.size() > 0);

        mNextEvent = TestEvents.REMOVED_SITE_PLUGINS;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PluginActionBuilder.newRemoveSitePluginsAction(site));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Assert site plugins are removed
        assertEquals(0, mPluginStore.getPluginDirectory(site, PluginDirectoryType.SITE).size());

        signOutWPCom();
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
    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        AppLog.d(T.TESTS, "Received OnAccountChanged event");
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
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
    public void onSiteRemoved(OnSiteRemoved event) {
        AppLog.d(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPluginDirectoryFetched(OnPluginDirectoryFetched event) {
        AppLog.i(T.API, "Received onPluginDirectoryFetched");
        if (event.isError()) {
            if (event.error.type.equals(PluginDirectoryErrorType.NOT_AVAILABLE)) {
                assertEquals(mNextEvent, TestEvents.PLUGIN_ACTION_NOT_AVAILABLE);
            } else {
                throw new AssertionError("Unexpected error occurred in onPluginDirectoryFetched with type: "
                                         + event.error.type);
            }
        } else {
            assertEquals(mNextEvent, TestEvents.SITE_PLUGINS_FETCHED);
            assertEquals(event.type, PluginDirectoryType.SITE);
            assertFalse(event.loadMore); // pagination is not enabled for site plugins
            assertFalse(event.canLoadMore); // pagination is not enabled for site plugins
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSitePluginConfigured(OnSitePluginConfigured event) {
        AppLog.i(T.API, "Received onSitePluginConfigured");
        if (event.isError()) {
            if (event.error.type.equals(ConfigureSitePluginErrorType.UNKNOWN_PLUGIN)) {
                assertEquals(mNextEvent, TestEvents.UNKNOWN_SITE_PLUGIN);
            } else if (event.error.type.equals(ConfigureSitePluginErrorType.NOT_AVAILABLE)) {
                assertEquals(mNextEvent, TestEvents.PLUGIN_ACTION_NOT_AVAILABLE);
            } else {
                throw new AssertionError("Unexpected error occurred in onSitePluginConfigured with type: "
                        + event.error.type);
            }
        } else {
            assertEquals(mNextEvent, TestEvents.CONFIGURED_SITE_PLUGIN);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSitePluginDeleted(OnSitePluginDeleted event) {
        AppLog.i(T.API, "Received onSitePluginDeleted");
        if (event.isError()) {
            if (event.error.type.equals(DeleteSitePluginErrorType.DELETE_PLUGIN_ERROR)) {
                assertEquals(mNextEvent, TestEvents.DELETE_SITE_PLUGIN_ERROR);
            } else if (event.error.type.equals(DeleteSitePluginErrorType.NOT_AVAILABLE)) {
                assertEquals(mNextEvent, TestEvents.PLUGIN_ACTION_NOT_AVAILABLE);
            } else {
                throw new AssertionError("Unexpected error occurred in onSitePluginDeleted with type: "
                        + event.error.type);
            }
        } else {
            assertEquals(mNextEvent, TestEvents.DELETED_SITE_PLUGIN);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSitePluginInstalled(OnSitePluginInstalled event) {
        AppLog.i(T.API, "Received onSitePluginInstalled");
        if (event.isError()) {
            if (event.error.type.equals(InstallSitePluginErrorType.NO_PACKAGE)) {
                assertEquals(mNextEvent, TestEvents.INSTALL_SITE_PLUGIN_ERROR_NO_PACKAGE);
            } else if (event.error.type.equals(InstallSitePluginErrorType.NOT_AVAILABLE)) {
                assertEquals(mNextEvent, TestEvents.PLUGIN_ACTION_NOT_AVAILABLE);
            } else {
                throw new AssertionError("Unexpected error occurred in onSitePluginInstalled with type: "
                        + event.error.type);
            }
            mCountDownLatch.countDown();
            return;
        }
        assertEquals(mNextEvent, TestEvents.INSTALLED_SITE_PLUGIN);

        // After a plugin is installed, we dispatch an event to activate it, so we need to wait for that to be completed
        // before the next actions can be taken. `mCountDownLatch.countDown()` should not be called as it'll be called
        // from onSitePluginConfigured once the activation is completed.
        mNextEvent = TestEvents.CONFIGURED_SITE_PLUGIN;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSitePluginsRemoved(OnSitePluginsRemoved event) {
        AppLog.i(T.API, "Received onSitePluginsRemoved");
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred in onSitePluginsRemoved");
        }
        assertEquals(mNextEvent, TestEvents.REMOVED_SITE_PLUGINS);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSitePluginUpdated(OnSitePluginUpdated event) {
        AppLog.i(T.API, "Received onSitePluginUpdated");
        if (event.isError()) {
            if (event.error.type.equals(UpdateSitePluginErrorType.NOT_AVAILABLE)) {
                assertEquals(mNextEvent, TestEvents.PLUGIN_ACTION_NOT_AVAILABLE);
            } else {
                throw new AssertionError("Unexpected error occurred in onSitePluginsRemoved");
            }
        }
        mCountDownLatch.countDown();
    }

    private void authenticateWPComAndFetchSites(String username, String password) throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AuthenticatePayload payload = new AuthenticatePayload(username, password);
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
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction(new FetchSitesPayload()));

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

    private SiteModel fetchSingleJetpackSitePlugins() throws InterruptedException {
        SiteModel site = authenticateAndRetrieveSingleJetpackSite();
        fetchSitePlugins(site, TestEvents.SITE_PLUGINS_FETCHED);
        return site;
    }

    private void fetchSitePlugins(SiteModel site, TestEvents testEvent) throws InterruptedException {
        mNextEvent = testEvent;
        mCountDownLatch = new CountDownLatch(1);
        FetchPluginDirectoryPayload payload = new FetchPluginDirectoryPayload(PluginDirectoryType.SITE, site, false);
        mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private SiteModel authenticateAndRetrieveSingleJetpackSite() throws InterruptedException {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_JETPACK_BETA_SITE,
                BuildConfig.TEST_WPCOM_PASSWORD_JETPACK_BETA_SITE);
        return mSiteStore.getSites().get(0);
    }

    private void configureSitePlugin(ConfigureSitePluginPayload payload,
                                     TestEvents testEvent) throws InterruptedException {
        mNextEvent = testEvent;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PluginActionBuilder.newConfigureSitePluginAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void deleteSitePlugin(SiteModel site, @NonNull ImmutablePluginModel plugin) throws InterruptedException {
        deleteSitePlugin(site, plugin, TestEvents.DELETED_SITE_PLUGIN);
    }

    private void deleteSitePlugin(SiteModel site, @NonNull ImmutablePluginModel plugin,
                                  TestEvents testEvent) throws InterruptedException {
        assertTrue(!TextUtils.isEmpty(plugin.getName()));
        assertTrue(!TextUtils.isEmpty(plugin.getSlug()));
        mNextEvent = testEvent;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PluginActionBuilder.newDeleteSitePluginAction(
                new DeleteSitePluginPayload(site, plugin.getName(), plugin.getSlug())));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void installSitePlugin(SiteModel site, String pluginSlug) throws InterruptedException {
        installSitePlugin(site, pluginSlug, TestEvents.INSTALLED_SITE_PLUGIN);
    }

    private void installSitePlugin(SiteModel site, String pluginSlug,
                                   TestEvents testEvent) throws InterruptedException {
        mNextEvent = testEvent;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PluginActionBuilder.newInstallSitePluginAction(
                new InstallSitePluginPayload(site, pluginSlug)));
        // Since after install we dispatch an event to activate the plugin, we are giving twice the normal time to
        // ensure there is enough time to complete both events
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS));
    }

    private void deactivatePlugin(SiteModel site, ImmutablePluginModel plugin) throws InterruptedException {
        ConfigureSitePluginPayload payload = new ConfigureSitePluginPayload(site, plugin.getName(), plugin.getSlug(),
                false, plugin.isAutoUpdateEnabled());
        configureSitePlugin(payload, TestEvents.CONFIGURED_SITE_PLUGIN);
    }

    private void updateSitePlugin(SiteModel site, String pluginName, String pluginSlug, TestEvents testEvent)
            throws InterruptedException {
        mNextEvent = testEvent;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PluginActionBuilder
                .newUpdateSitePluginAction(new UpdateSitePluginPayload(site, pluginName, pluginSlug)));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
