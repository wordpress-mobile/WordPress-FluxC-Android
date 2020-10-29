package org.wordpress.android.fluxc.wc

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils.WCPluginModel
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCPluginSqlUtilsTest {
    private val site = SiteModel().apply { id = 2 }
    private val testPlugins = listOf(
            WCPluginModel(
                    1,
                    site.id,
                    true,
                    "Plugin 1",
                    "plugin1",
                    "1.0"
            ),
            WCPluginModel(
                    2,
                    site.id,
                    false,
                    "Plugin 2",
                    "plugin2",
                    "2.0"
            )
    )

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCPluginModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun `test plugin insert`() {
        WCPluginSqlUtils.insertOrUpdate(testPlugins)
        val plugins = WCPluginSqlUtils.selectAll(site)
        assertEquals(2, plugins.size)
        assertEquals(testPlugins, plugins)
    }

    @Test
    fun `test gateway update`() {
        val testPlugin = testPlugins.first()
        WCPluginSqlUtils.insertOrUpdate(listOf(testPlugin))
        val plugin = WCPluginSqlUtils.selectSingle(site, testPlugin.slug)!!
        assertEquals(plugin, testPlugin)

        val newTitle = "New title"
        WCPluginSqlUtils.insertOrUpdate(listOf(plugin.copy(displayName = newTitle)))
        val updatedPlugin = WCPluginSqlUtils.selectSingle(site, plugin.slug)!!
        assertEquals(newTitle, updatedPlugin.displayName)
    }

    @Test
    fun `test select`() {
        WCPluginSqlUtils.insertOrUpdate(testPlugins)

        val gateway = WCPluginSqlUtils.selectSingle(site, "plugin2")
        assertEquals(testPlugins[1], gateway)
    }

    @Test
    fun `test select empty result`() {
        WCPluginSqlUtils.insertOrUpdate(testPlugins.map { it.copy(localSiteId = 3) })
        val plugins = WCPluginSqlUtils.selectAll(site)
        assertTrue(plugins.isEmpty())

        val plugin = WCPluginSqlUtils.selectSingle(site, testPlugins.first().slug)
        assertNull(plugin)
    }

    @Test
    fun `test select after item deleted`() {
        WCPluginSqlUtils.insertOrUpdate(testPlugins)
        val plugins = WCPluginSqlUtils.selectAll(site)
        assertEquals(testPlugins.size, plugins.count())

        WCPluginSqlUtils.insertOrUpdate(listOf(testPlugins.first()))
        val listOfOne = WCPluginSqlUtils.selectAll(site)
        assertEquals(1, listOfOne.count())
        val missing = WCPluginSqlUtils.selectSingle(site, testPlugins[1].slug)
        assertNull(missing)
    }
}
