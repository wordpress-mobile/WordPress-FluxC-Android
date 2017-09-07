package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient.BrowsePluginPayload;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient.FetchedPluginInfoPayload;
import org.wordpress.android.fluxc.store.PluginStore.UpdatePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchedPluginsPayload;
import org.wordpress.android.fluxc.store.PluginStore.UpdatedPluginPayload;

@ActionEnum
public enum PluginAction implements IAction {
    // Remote REST actions
    @Action(payloadType = SiteModel.class)
    FETCH_PLUGINS,
    @Action(payloadType = UpdatePluginPayload.class)
    UPDATE_PLUGIN,

    // REMOTE WPORG actions
    @Action(payloadType = String.class)
    FETCH_PLUGIN_INFO,
    @Action(payloadType = BrowsePluginPayload.class)
    FETCH_WPORG_PLUGINS,

    // Remote REST responses
    @Action(payloadType = FetchedPluginsPayload.class)
    FETCHED_PLUGINS,
    @Action(payloadType = UpdatedPluginPayload.class)
    UPDATED_PLUGIN,

    // Remote WPORG responses
    @Action(payloadType = FetchedPluginInfoPayload.class)
    FETCHED_PLUGIN_INFO,
}
