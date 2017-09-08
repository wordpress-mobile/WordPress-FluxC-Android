package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient.FetchPluginDirectoryPayload;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient.FetchedPluginInfoPayload;
import org.wordpress.android.fluxc.store.PluginStore.UpdatePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchedSitePluginsPayload;
import org.wordpress.android.fluxc.store.PluginStore.UpdatedPluginPayload;

@ActionEnum
public enum PluginAction implements IAction {
    // Remote REST actions
    @Action(payloadType = SiteModel.class)
    FETCH_SITE_PLUGINS,
    @Action(payloadType = UpdatePluginPayload.class)
    UPDATE_PLUGIN,

    // REMOTE WPORG actions
    @Action(payloadType = String.class)
    FETCH_PLUGIN_INFO,
    @Action(payloadType = FetchPluginDirectoryPayload.class)
    FETCH_PLUGIN_DIRECTORY,

    // Remote REST responses
    @Action(payloadType = FetchedSitePluginsPayload.class)
    FETCHED_SITE_PLUGINS,
    @Action(payloadType = UpdatedPluginPayload.class)
    UPDATED_PLUGIN,

    // Remote WPORG responses
    @Action(payloadType = FetchedPluginInfoPayload.class)
    FETCHED_PLUGIN_INFO,
}
