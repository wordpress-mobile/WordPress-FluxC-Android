package org.wordpress.android.fluxc.example

import android.app.Fragment
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ThemeActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.fluxc.store.SiteStore

import javax.inject.Inject

class ThemeFragment : Fragment() {
    @Inject internal var mSiteStore: SiteStore? = null
    @Inject internal var mThemeStore: ThemeStore? = null
    @Inject internal var mDispatcher: Dispatcher? = null

    private val wpComSite: SiteModel?
        get() {
            for (site in mSiteStore!!.sites) {
                if (site != null && site.isWPCom) {
                    return site
                }
            }
            return null
        }

    private val jetpackConnectedSite: SiteModel?
        get() {
            for (site in mSiteStore!!.sites) {
                if (site != null && site.isJetpackConnected) {
                    return site
                }
            }
            return null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity.application as ExampleApp).component.inject(this)
    }

    override fun onStart() {
        super.onStart()
        mDispatcher!!.register(this)
    }

    override fun onStop() {
        super.onStop()
        mDispatcher!!.unregister(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_themes, container, false)

        view.findViewById(R.id.activate_theme_wpcom).setOnClickListener(View.OnClickListener {
            val id = getThemeIdFromInput(view)
            if (TextUtils.isEmpty(id)) {
                prependToLog("Please enter a theme id")
                return@OnClickListener
            }

            val site = wpComSite
            if (site == null) {
                prependToLog("No WP.com site found, unable to test.")
            } else {
                val theme = ThemeModel()
                theme.localSiteId = site.id
                theme.themeId = id
                val payload = ThemeStore.ActivateThemePayload(site, theme)
                mDispatcher!!.dispatch(ThemeActionBuilder.newActivateThemeAction(payload))
            }
        })

        view.findViewById(R.id.activate_theme_jp).setOnClickListener(View.OnClickListener {
            val id = getThemeIdFromInput(view)
            if (TextUtils.isEmpty(id)) {
                prependToLog("Please enter a theme id")
                return@OnClickListener
            }

            val site = jetpackConnectedSite
            if (site == null) {
                prependToLog("No Jetpack connected site found, unable to test.")
            } else {
                val theme = ThemeModel()
                theme.localSiteId = site.id
                theme.themeId = id
                val payload = ThemeStore.ActivateThemePayload(site, theme)
                mDispatcher!!.dispatch(ThemeActionBuilder.newActivateThemeAction(payload))
            }
        })

        view.findViewById(R.id.install_theme_jp).setOnClickListener(View.OnClickListener {
            val id = getThemeIdFromInput(view)
            if (TextUtils.isEmpty(id)) {
                prependToLog("Please enter a theme id")
                return@OnClickListener
            }

            val site = jetpackConnectedSite
            if (site == null) {
                prependToLog("No Jetpack connected site found, unable to test.")
            } else {
                val theme = ThemeModel()
                theme.localSiteId = site.id
                theme.themeId = id
                val payload = ThemeStore.ActivateThemePayload(site, theme)
                mDispatcher!!.dispatch(ThemeActionBuilder.newInstallThemeAction(payload))
            }
        })

        view.findViewById(R.id.search_themes).setOnClickListener(View.OnClickListener {
            val term = getThemeIdFromInput(view)
            if (TextUtils.isEmpty(term)) {
                prependToLog("Please enter a search term")
                return@OnClickListener
            }

            val payload = ThemeStore.SearchThemesPayload(term!!)
            mDispatcher!!.dispatch(ThemeActionBuilder.newSearchThemesAction(payload))
        })

        view.findViewById(R.id.delete_theme_jp).setOnClickListener(View.OnClickListener {
            val id = getThemeIdFromInput(view)
            if (TextUtils.isEmpty(id)) {
                prependToLog("Please enter a theme id")
                return@OnClickListener
            }

            val site = jetpackConnectedSite
            if (site == null) {
                prependToLog("No Jetpack connected site found, unable to test.")
            } else {
                val theme = ThemeModel()
                theme.localSiteId = site.id
                theme.themeId = id
                val payload = ThemeStore.ActivateThemePayload(site, theme)
                mDispatcher!!.dispatch(ThemeActionBuilder.newDeleteThemeAction(payload))
            }
        })

        view.findViewById(R.id.fetch_wpcom_themes).setOnClickListener { mDispatcher!!.dispatch(ThemeActionBuilder.newFetchWpComThemesAction()) }

        view.findViewById(R.id.fetch_installed_themes).setOnClickListener {
            val jetpackSite = jetpackConnectedSite
            if (jetpackSite == null) {
                prependToLog("No Jetpack connected site found, unable to test.")
            } else {
                mDispatcher!!.dispatch(ThemeActionBuilder.newFetchInstalledThemesAction(jetpackSite))
            }
        }

        view.findViewById(R.id.fetch_current_theme_jp).setOnClickListener {
            val jetpackSite = jetpackConnectedSite
            if (jetpackSite == null) {
                prependToLog("No Jetpack connected site found, unable to test.")
            } else {
                mDispatcher!!.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(jetpackSite))
            }
        }

        view.findViewById(R.id.fetch_current_theme_wpcom).setOnClickListener {
            val wpcomSite = wpComSite
            if (wpcomSite == null) {
                prependToLog("No WP.com site found, unable to test.")
            } else {
                mDispatcher!!.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(wpcomSite))
            }
        }

        return view
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
            prependToLog("success: WP.com theme count = " + mThemeStore!!.wpComThemes.size)
            val jpSite = jetpackConnectedSite
            if (jpSite != null) {
                val themes = mThemeStore!!.getThemesForSite(jpSite)
                prependToLog("Installed theme count = " + themes.size)
            }
        }
    }

    private fun prependToLog(s: String) {
        (activity as MainExampleActivity).prependToLog(s)
    }

    private fun getThemeIdFromInput(root: View?): String? {
        val themeIdInput = if (root == null) null else root.findViewById(R.id.theme_id) as TextView
        return themeIdInput?.text?.toString()
    }
}
