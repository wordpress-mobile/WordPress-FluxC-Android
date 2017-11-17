package org.wordpress.android.fluxc.example

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.fragment_themes.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ThemeActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.ThemeStore
import javax.inject.Inject

class ThemeFragment : Fragment() {
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var themeStore: ThemeStore
    @Inject internal lateinit var dispatcher: Dispatcher

    override fun onAttach(context: Context?) {
        AndroidInjection.inject(this)
        super.onAttach(context)
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.fragment_themes, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activate_theme_wpcom.setOnClickListener {
            val id = getThemeIdFromInput(view)
            if (TextUtils.isEmpty(id)) {
                prependToLog("Please enter a theme id")
            } else {
                val site = getWpComSite()
                if (site == null) {
                    prependToLog("No WP.com site found, unable to test.")
                } else {
                    val theme = ThemeModel()
                    theme.localSiteId = site.id
                    theme.themeId = id
                    val payload = ThemeStore.ActivateThemePayload(site, theme)
                    dispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(payload))
                }
            }
        }

        activate_theme_jp.setOnClickListener {
            val id = getThemeIdFromInput(view)
            if (TextUtils.isEmpty(id)) {
                prependToLog("Please enter a theme id")
            } else {
                val site = getJetpackConnectedSite()
                if (site == null) {
                    prependToLog("No Jetpack connected site found, unable to test.")
                } else {
                    val theme = ThemeModel()
                    theme.localSiteId = site.id
                    theme.themeId = id
                    val payload = ThemeStore.ActivateThemePayload(site, theme)
                    dispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(payload))
                }
            }
        }

        install_theme_jp.setOnClickListener {
            val id = getThemeIdFromInput(view)
            if (TextUtils.isEmpty(id)) {
                prependToLog("Please enter a theme id")
            } else {
                val site = getJetpackConnectedSite()
                if (site == null) {
                    prependToLog("No Jetpack connected site found, unable to test.")
                } else {
                    val theme = ThemeModel()
                    theme.localSiteId = site.id
                    theme.themeId = id
                    val payload = ThemeStore.ActivateThemePayload(site, theme)
                    dispatcher.dispatch(ThemeActionBuilder.newInstallThemeAction(payload))
                }
            }
        }

        search_themes.setOnClickListener {
            val term = getThemeIdFromInput(view)
            if (TextUtils.isEmpty(term)) {
                prependToLog("Please enter a search term")
            } else {
                dispatcher.dispatch(ThemeActionBuilder.newSearchThemesAction(ThemeStore.SearchThemesPayload(term)))
            }
        }

        delete_theme_jp.setOnClickListener {
            val id = getThemeIdFromInput(view)
            if (TextUtils.isEmpty(id)) {
                prependToLog("Please enter a theme id")
            } else {
                val site = getJetpackConnectedSite()
                if (site == null) {
                    prependToLog("No Jetpack connected site found, unable to test.")
                } else {
                    val theme = ThemeModel()
                    theme.localSiteId = site.id
                    theme.themeId = id
                    val payload = ThemeStore.ActivateThemePayload(site, theme)
                    dispatcher.dispatch(ThemeActionBuilder.newDeleteThemeAction(payload))
                }
            }
        }

        fetch_wpcom_themes.setOnClickListener {
            dispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction())
        }

        fetch_installed_themes.setOnClickListener {
            if (getJetpackConnectedSite() == null) {
                prependToLog("No Jetpack connected site found, unable to test.")
            } else {
                dispatcher.dispatch(ThemeActionBuilder.newFetchInstalledThemesAction(getJetpackConnectedSite()))
            }
        }

        fetch_current_theme_jp.setOnClickListener {
            if (getJetpackConnectedSite() == null) {
                prependToLog("No Jetpack connected site found, unable to test.")
            } else {
                dispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(getJetpackConnectedSite()))
            }
        }

        fetch_current_theme_wpcom.setOnClickListener {
            if (getWpComSite() == null) {
                prependToLog("No WP.com site found, unable to test.")
            } else {
                dispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(getWpComSite()))
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onThemeInstalled(event: ThemeStore.OnThemeInstalled) {
        prependToLog("onThemeInstalled: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            prependToLog("success: theme = " + event.theme.themeId)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onThemeDeleted(event: ThemeStore.OnThemeDeleted) {
        prependToLog("onThemeDeleted: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            prependToLog("success: theme = " + event.theme.themeId)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onThemeActivated(event: ThemeStore.OnThemeActivated) {
        prependToLog("onThemeActivated: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            prependToLog("success: theme = " + event.theme.themeId)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCurrentThemeFetched(event: ThemeStore.OnCurrentThemeFetched) {
        prependToLog("onCurrentThemeFetched: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            prependToLog("success: theme = " + event.theme.name)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onThemesSearched(event: ThemeStore.OnThemesSearched) {
        prependToLog("onThemesSearched: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            prependToLog("success: result count = " + event.searchResults.size)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onThemesChanged(event: ThemeStore.OnThemesChanged) {
        prependToLog("onThemesChanged: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            prependToLog("success: WP.com theme count = " + themeStore.wpComThemes.size)
            val jpSite = getJetpackConnectedSite()
            if (jpSite != null) {
                val themes = themeStore.getThemesForSite(jpSite)
                prependToLog("Installed theme count = " + themes.size)
            }
        }
    }

    private fun prependToLog(s: String) = (activity as MainExampleActivity).prependToLog(s)

    private fun getThemeIdFromInput(root: View?): String {
        val themeIdInput = if (root == null) null else root.findViewById(R.id.theme_id) as TextView
        return themeIdInput?.text?.toString() ?: ""
    }

    private fun getWpComSite(): SiteModel? {
        return siteStore.sites.firstOrNull { it != null && it.isWPCom }
    }

    private fun getJetpackConnectedSite(): SiteModel? {
        return siteStore.sites.firstOrNull { it != null && it.isJetpackConnected }
    }
}
