package org.wordpress.android.fluxc.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_plugins.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.PluginActionBuilder
import org.wordpress.android.fluxc.store.PluginStore
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginFetched
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled
import javax.inject.Inject

class PluginsFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var pluginStore: PluginStore

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_plugins, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

                showSingleLineDialog(activity, "Enter a plugin slug.") { pluginSlugText ->
                    if (pluginSlugText.text.isEmpty()) {
                        prependToLog("Slug is null so doing nothing")
                        return@showSingleLineDialog
                    }
                    pluginSlugText.text.toString().apply {
                        prependToLog("Fetching plugin: $this")

                        val payload = FetchSitePluginPayload(site, this)
                        dispatcher.dispatch(PluginActionBuilder.newFetchSitePluginAction(payload))
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
    fun onSitePluginFetched(event: OnSitePluginFetched) {
        if (!event.isError) {
            prependToLog("${event.plugin.displayName}: ${event.plugin.description}")
        } else {
            prependToLog("Fetching failed: ${event.error.type}")
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginConfigured(event: OnSitePluginConfigured) {
        if (!event.isError) {
            // If there is no error, we can assume that the configuration (activating in our case) is successful.
            prependToLog("${event.pluginName} is activated.")
        }
    }
}
