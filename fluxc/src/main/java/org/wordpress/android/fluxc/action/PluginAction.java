package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient.FetchedSitePluginsPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient.UpdateSitePluginPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient.UpdatedSitePluginPayload;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient.FetchPluginDirectoryPayload;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient.FetchedPluginInfoPayload;

@ActionEnum
public enum PluginAction implements IAction {
    // Remote REST actions
    @Action(payloadType = SiteModel.class)
    FETCH_SITE_PLUGINS,
    @Action(payloadType = UpdateSitePluginPayload.class)
    UPDATE_SITE_PLUGIN,

    // REMOTE WPORG actions
    @Action(payloadType = String.class)
    FETCH_PLUGIN_INFO,
    @Action(payloadType = FetchPluginDirectoryPayload.class)
    FETCH_PLUGIN_DIRECTORY,

    // Remote REST responses
    @Action(payloadType = FetchedSitePluginsPayload.class)
    FETCHED_SITE_PLUGINS,
    @Action(payloadType = UpdatedSitePluginPayload.class)
    UPDATED_SITE_PLUGIN,

    // Remote WPORG responses
    @Action(payloadType = FetchedPluginInfoPayload.class)
    FETCHED_PLUGIN_INFO,
}
