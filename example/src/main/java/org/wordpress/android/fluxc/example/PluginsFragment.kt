package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_notifications.*
import kotlinx.android.synthetic.main.fragment_plugins.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.ui.common.showSiteSelectorDialog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.PluginActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PluginStore
import org.wordpress.android.fluxc.store.PluginStore.FetchJetpackSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.OnJetpackSitePluginFetched
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled
import javax.inject.Inject

class PluginsFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var pluginStore: PluginStore

    private var selectedSite: SiteModel? = null
    private var selectedPos: Int = -1

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_plugins, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        plugins_select_site.setOnClickListener {
            showSiteSelectorDialog(selectedPos, object : SiteSelectorDialog.Listener {
                override fun onSiteSelected(site: SiteModel, pos: Int) {
                    selectedSite = site
                    selectedPos = pos
                    plugins_selected_site.text = site.name ?: site.displayName
                }
            })
        }

        install_activate_plugin.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter a plugin slug.\n(Hint: If testing Jetpack CP sites, don't use `jetpack`)"
                ) { pluginSlugText ->
                    if (pluginSlugText.text.isEmpty()) {
                        prependToLog("Slug is null so doing nothing")
                        return@showSingleLineDialog
                    }
                    pluginSlugText.text.toString().apply {
                        prependToLog("Installing plugin: $this")
                        val payload = InstallSitePluginPayload(site, this)
                        dispatcher.dispatch(PluginActionBuilder.newInstallSitePluginAction(payload))
                    }
                }
            } ?: prependToLog("Please select a site first.")
        }

        fetch_plugin.setOnClickListener {
            selectedSite?.let { site ->

                showSingleLineDialog(activity, "Enter the plugin name.") { pluginNameText ->
                    if (pluginNameText.text.isEmpty()) {
                        prependToLog("Name is null so doing nothing")
                        return@showSingleLineDialog
                    }
                    pluginNameText.text.toString().apply {
                        prependToLog("Fetching plugin: $this")

                        val payload = FetchJetpackSitePluginPayload(site, this)
                        dispatcher.dispatch(PluginActionBuilder.newFetchJetpackSitePluginAction(payload))
                    }
                }
            } ?: prependToLog("Please select a site first.")
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginInstalled(event: OnSitePluginInstalled) {
        if (!event.isError) {
            prependToLog("${event.slug} is installed to ${event.site.name}.")
        } else {
            event.error.message?.let {
                prependToLog("Installation failed: $it")
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginFetched(event: OnJetpackSitePluginFetched) {
        if (!event.isError) {
            prependToLog("${event.plugin.displayName}: ${event.plugin.description}")
        } else {
            prependToLog("Fetching failed: ${event.error.type}")
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginConfigured(event: OnSitePluginConfigured) {
        if (!event.isError) {
            // If there is no error, we can assume that the configuration (activating in our case) is successful.
            prependToLog("${event.pluginName} is activated.")
        }
    }
}
