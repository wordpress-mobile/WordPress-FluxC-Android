package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.databinding.FragmentThemesBinding
import org.wordpress.android.fluxc.example.ui.common.showSiteSelectorDialog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.ThemeActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.ThemeCoroutineStore
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.fluxc.store.ThemeStore.FetchWPComThemesPayload
import org.wordpress.android.fluxc.store.ThemeStore.OnCurrentThemeFetched
import org.wordpress.android.fluxc.store.ThemeStore.OnSiteThemesChanged
import org.wordpress.android.fluxc.store.ThemeStore.OnThemeActivated
import org.wordpress.android.fluxc.store.ThemeStore.OnThemeDeleted
import org.wordpress.android.fluxc.store.ThemeStore.OnThemeInstalled
import org.wordpress.android.fluxc.store.ThemeStore.OnWpComThemesChanged
import javax.inject.Inject

class ThemeFragment : Fragment() {
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var themeStore: ThemeStore
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var themeCoroutineStore: ThemeCoroutineStore

    private var selectedSite: SiteModel? = null
    private var selectedPos: Int = -1

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentThemesBinding.inflate(inflater, container, false).root

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentThemesBinding.bind(view)) {
            themeSelectSite.setOnClickListener {
                showSiteSelectorDialog(selectedPos, object : SiteSelectorDialog.Listener {
                    override fun onSiteSelected(site: SiteModel, pos: Int) {
                        selectedSite = site
                        selectedPos = pos
                        val wpcomSite = if (site.isWPCom) "WP" else ""
                        val jetpackSite = if (site.isJetpackConnected) "JP" else ""
                        val siteName = site.name ?: site.displayName
                        val selectedSite = "[$wpcomSite$jetpackSite] $siteName"
                        themeSelectedSite.text = selectedSite
                    }
                })
            }

            activateThemeWpcom.setOnClickListener {
                val id = themeId.text.toString()
                if (TextUtils.isEmpty(id)) {
                    prependToLog("Please enter a theme id")
                } else {
                    val site = getWpComSite()
                    if (site == null) {
                        prependToLog("No WP.com site found, unable to test.")
                    } else {
                        @Suppress("DEPRECATION") val theme = ThemeModel()
                        theme.localSiteId = site.id
                        theme.themeId = id
                        val payload = ThemeStore.SiteThemePayload(site, theme)
                        dispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(payload))
                    }
                }
            }

            activateThemeJp.setOnClickListener {
                val id = themeId.text.toString()
                if (TextUtils.isEmpty(id)) {
                    prependToLog("Please enter a theme id")
                } else {
                    val site = getJetpackConnectedSite()
                    if (site == null) {
                        prependToLog("No Jetpack connected site found, unable to test.")
                    } else {
                        @Suppress("DEPRECATION") val theme = ThemeModel()
                        theme.localSiteId = site.id
                        theme.themeId = id
                        val payload = ThemeStore.SiteThemePayload(site, theme)
                        dispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(payload))
                    }
                }
            }

            installThemeJp.setOnClickListener {
                val id = themeId.text.toString()
                if (TextUtils.isEmpty(id)) {
                    prependToLog("Please enter a theme id")
                } else {
                    val site = getJetpackConnectedSite()
                    if (site == null) {
                        prependToLog("No Jetpack connected site found, unable to test.")
                    } else {
                        @Suppress("DEPRECATION") val theme = ThemeModel()
                        theme.localSiteId = site.id
                        theme.themeId = id
                        val payload = ThemeStore.SiteThemePayload(site, theme)
                        dispatcher.dispatch(ThemeActionBuilder.newInstallThemeAction(payload))
                    }
                }
            }

            deleteThemeJp.setOnClickListener {
                val id = themeId.text.toString()
                if (TextUtils.isEmpty(id)) {
                    prependToLog("Please enter a theme id")
                } else {
                    val site = getJetpackConnectedSite()
                    if (site == null) {
                        prependToLog("No Jetpack connected site found, unable to test.")
                    } else {
                        @Suppress("DEPRECATION") val theme = ThemeModel()
                        theme.localSiteId = site.id
                        theme.themeId = id
                        val payload = ThemeStore.SiteThemePayload(site, theme)
                        dispatcher.dispatch(ThemeActionBuilder.newDeleteThemeAction(payload))
                    }
                }
            }

            fetchWpcomThemes.setOnClickListener {
                lifecycleScope.launch {
                    val filter = showSingleLineDialog(
                        activity = requireActivity(),
                        message = "Enter filter (Optional)",
                        isNumeric = false
                    )
                    val limit = showSingleLineDialog(
                        activity = requireActivity(),
                        message = "Limit results? (Optional, defaults to 500)",
                        isNumeric = true
                    )
                    dispatcher.dispatch(
                        ThemeActionBuilder.newFetchWpComThemesAction(
                            limit?.let {
                                FetchWPComThemesPayload(filter, it.toInt())
                            } ?: FetchWPComThemesPayload(filter)
                        )
                    )
                }
            }

            fetchInstalledThemes.setOnClickListener {
                if (getJetpackConnectedSite() == null) {
                    prependToLog("No Jetpack connected site found, unable to test.")
                } else {
                    dispatcher.dispatch(
                        ThemeActionBuilder.newFetchInstalledThemesAction(
                            getJetpackConnectedSite()
                        )
                    )
                }
            }

            fetchCurrentThemeJp.setOnClickListener {
                if (getJetpackConnectedSite() == null) {
                    prependToLog("No Jetpack connected site found, unable to test.")
                } else {
                    dispatcher.dispatch(
                        ThemeActionBuilder.newFetchCurrentThemeAction(
                            getJetpackConnectedSite()
                        )
                    )
                }
            }

            fetchCurrentThemeWpcom.setOnClickListener {
                if (getWpComSite() == null) {
                    prependToLog("No WP.com site found, unable to test.")
                } else {
                    dispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(getWpComSite()))
                }
            }

            fetchDemoThemesPages.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    val response = themeCoroutineStore.fetchDemoThemePages("https://zainodemo.wpcomstaging.com")
                    withContext(Dispatchers.Main) {
                        prependToLog("Demo themes pages fetched: $response")
                    }
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onThemeInstalled(event: OnThemeInstalled) {
        prependToLog("onThemeInstalled: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            prependToLog("success: theme = " + event.theme.themeId)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onThemeDeleted(event: OnThemeDeleted) {
        prependToLog("onThemeDeleted: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            prependToLog("success: theme = " + event.theme.themeId)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onThemeActivated(event: OnThemeActivated) {
        prependToLog("onThemeActivated: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            prependToLog("success: theme = " + event.theme.themeId)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCurrentThemeFetched(event: OnCurrentThemeFetched) {
        prependToLog("onCurrentThemeFetched: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            prependToLog("success: theme = " + event.theme?.name)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteThemesChanged(event: OnSiteThemesChanged) {
        prependToLog("onSiteThemesChanged: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            val jpSite = getJetpackConnectedSite()
            if (jpSite != null) {
                val themes = themeStore.getThemesForSite(jpSite)
                prependToLog("Installed theme count = " + themes.size)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWpComThemesChanged(event: OnWpComThemesChanged) {
        prependToLog("onWpComThemesChanged: ")
        if (event.isError) {
            prependToLog("error: " + event.error.message)
        } else {
            prependToLog("success: WP.com themes fetched count = " + themeStore.wpComThemes.size)

            listOf(
                ThemeStore.MOBILE_FRIENDLY_CATEGORY_BLOG,
                ThemeStore.MOBILE_FRIENDLY_CATEGORY_WEBSITE,
                ThemeStore.MOBILE_FRIENDLY_CATEGORY_PORTFOLIO
            ).forEach { category ->
                val mobileFriendlyThemes = themeStore.getWpComMobileFriendlyThemes(category)
                prependToLog(category + " theme count = " + mobileFriendlyThemes.size)
                mobileFriendlyThemes.forEach { theme ->
                    prependToLog(category + " theme: " + theme.name)
                }
            }
        }
    }

    private fun prependToLog(s: String) = (activity as MainExampleActivity).prependToLog(s)

    private fun getWpComSite(): SiteModel? {
        return if (selectedSite?.isWPCom == true) {
            selectedSite
        } else {
            null
        }
    }

    private fun getJetpackConnectedSite(): SiteModel? {
        return if (selectedSite?.isJetpackConnected == true) {
            selectedSite
        } else {
            null
        }
    }
}
