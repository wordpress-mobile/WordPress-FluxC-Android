package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.example.test.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.persistence.PluginSqlUtils
import org.wordpress.android.fluxc.store.PluginCoroutineStore
import javax.inject.Inject

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
class ReleaseStack_WPApiPluginTest : ReleaseStack_Base() {
    private val incomingActions: MutableList<Action<*>> = mutableListOf()
    @Inject lateinit var pluginCoroutineStore: PluginCoroutineStore
    private val pluginSlug = "hello-dolly"

    private var nextEvent: TestEvents? = null

    internal enum class TestEvents {
        NONE,
        SITE_CHANGED,
        SITE_REMOVED
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
        // Reset expected test event
        nextEvent = TestEvents.NONE
        this.incomingActions.clear()
    }

    @Test
    fun testFetchPlugins() {
        val site = initSite()

        runBlocking {
            val fetchedPlugins = pluginCoroutineStore.syncFetchWPApiPlugins(site)

            assertNotNull(fetchedPlugins)
            assertFalse(fetchedPlugins.isError)

            val pluginsFromDb = PluginSqlUtils.getSitePlugins(site)

            assertNotNull(pluginsFromDb)
            assertTrue(pluginsFromDb.isNotEmpty())
        }
    }

    @Test
    @Ignore
    fun testActivateAndDeactivatePlugins() {
        val site = initSite()

        runBlocking {
            val activePlugin = getActivePlugin(site)

            val deactivatedPlugin = deactivatePlugin(site, activePlugin)

            activatePlugin(site, deactivatedPlugin)
        }
    }

    @Test
    @Ignore
    fun testUninstallAndInstallPlugin() {
        val site = initSite()

        runBlocking {
            val activePlugin = getActivePlugin(site)

            val deactivatedPlugin = deactivatePlugin(site, activePlugin)

            deletePlugin(site, deactivatedPlugin)

            installPlugin(site)
        }
    }

    private suspend fun activatePlugin(
        site: SiteModel,
        plugin: SitePluginModel
    ): SitePluginModel {
        pluginCoroutineStore.syncConfigureSitePlugin(site, plugin.name, plugin.slug, true)

        val activatedPlugin = PluginSqlUtils.getSitePluginBySlug(site, plugin.slug)

        assertTrue(activatedPlugin.isActive)
        return activatedPlugin
    }

    private suspend fun deactivatePlugin(
        site: SiteModel,
        plugin: SitePluginModel
    ): SitePluginModel {
        pluginCoroutineStore.syncConfigureSitePlugin(site, plugin.name, plugin.slug, false)

        val deactivatedPlugin = PluginSqlUtils.getSitePluginBySlug(site, plugin.slug)

        assertFalse(deactivatedPlugin.isActive)
        return deactivatedPlugin
    }

    private suspend fun installPlugin(
        site: SiteModel
    ): SitePluginModel {
        val result = pluginCoroutineStore.syncInstallSitePlugin(site, pluginSlug)

        assertFalse(result.isError)

        val installedPlugin = PluginSqlUtils.getSitePluginBySlug(site, pluginSlug)

        assertNotNull(installedPlugin)
        assertTrue(installedPlugin.isActive)

        return installedPlugin
    }

    private suspend fun deletePlugin(
        site: SiteModel,
        plugin: SitePluginModel
    ) {
        pluginCoroutineStore.syncDeleteSitePlugin(site, plugin.name, plugin.slug)

        val deletedPlugin = PluginSqlUtils.getSitePluginBySlug(site, plugin.slug)

        assertNull(deletedPlugin)
    }

    private suspend fun getActivePlugin(site: SiteModel): SitePluginModel {
        val fetchedPlugins = pluginCoroutineStore.syncFetchWPApiPlugins(site)

        assertNotNull(fetchedPlugins)
        assertFalse(fetchedPlugins.isError)

        val plugin = PluginSqlUtils.getSitePluginBySlug(site, pluginSlug)
        return plugin?.let {
            if (!it.isActive) {
                activatePlugin(site, plugin)
            } else {
                it
            }
        } ?: installPlugin(site)
    }

    @Subscribe
    fun onAction(action: Action<*>) {
        if (action.type is ActivityLogAction) {
            incomingActions.add(action)
            mCountDownLatch?.countDown()
        }
    }

    private fun initSite(): SiteModel {
        val site = SiteModel()
        site.url = BuildConfig.TEST_WPORG_URL_SINGLE_JETPACK_ONLY
        site.username = BuildConfig.TEST_WPORG_USERNAME_SINGLE_JETPACK_ONLY
        site.password = BuildConfig.TEST_WPORG_PASSWORD_SINGLE_JETPACK_ONLY
        return site
    }
}
