package org.wordpress.android.fluxc.store;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.PluginAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient.FetchSitePluginsError;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient.FetchedSitePluginsPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient.UpdateSitePluginError;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient.UpdateSitePluginPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient.UpdatedSitePluginPayload;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient.FetchPluginInfoError;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient.FetchedPluginDirectoryPayload;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient.FetchedPluginInfoPayload;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;

public class PluginStore extends Store {
    public enum FetchPluginsErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        NOT_AVAILABLE // Return for non-jetpack sites
    }

    public enum FetchPluginInfoErrorType {
        GENERIC_ERROR
    }

    public enum UpdateSitePluginErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        NOT_AVAILABLE // Return for non-jetpack sites
    }

    public static class OnSitePluginsChanged extends OnChanged<FetchSitePluginsError> {
        public SiteModel site;
        public OnSitePluginsChanged(SiteModel site) {
            this.site = site;
        }
    }

    public static class OnPluginInfoChanged extends OnChanged<FetchPluginInfoError> {
        public PluginInfoModel pluginInfo;
    }

    public static class OnPluginDirectoryChanged extends OnChanged<FetchPluginInfoError> {
        public int page;
    }

    public static class OnSitePluginChanged extends OnChanged<UpdateSitePluginError> {
        public SiteModel site;
        public PluginModel plugin;
        public OnSitePluginChanged(SiteModel site) {
            this.site = site;
        }
    }

    private final PluginRestClient mPluginRestClient;
    private final PluginWPOrgClient mPluginWPOrgClient;

    @Inject
    public PluginStore(Dispatcher dispatcher, PluginRestClient pluginRestClient, PluginWPOrgClient pluginWPOrgClient) {
        super(dispatcher);
        mPluginRestClient = pluginRestClient;
        mPluginWPOrgClient = pluginWPOrgClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "PluginStore onRegister");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof PluginAction)) {
            return;
        }
        switch ((PluginAction) actionType) {
            // REST actions
            case FETCH_SITE_PLUGINS:
                fetchSitePlugins((SiteModel) action.getPayload());
                break;
            case UPDATE_SITE_PLUGIN:
                updateSitePlugin((UpdateSitePluginPayload) action.getPayload());
                break;
            // WPORG actions
            case FETCH_PLUGIN_INFO:
                fetchPluginInfo((String) action.getPayload());
                break;
            case FETCH_PLUGIN_DIRECTORY:
                fetchPluginDirectory((PluginWPOrgClient.FetchPluginDirectoryPayload) action.getPayload());
                break;
            // REST responses
            case FETCHED_SITE_PLUGINS:
                fetchedSitePlugins((FetchedSitePluginsPayload) action.getPayload());
                break;
            case UPDATED_SITE_PLUGIN:
                updatedSitePlugin((UpdatedSitePluginPayload) action.getPayload());
                break;
            // WPORG responses
            case FETCHED_PLUGIN_INFO:
                fetchedPluginInfo((FetchedPluginInfoPayload) action.getPayload());
                break;
            case FETCHED_PLUGIN_DIRECTORY:
                fetchedPluginDirectory((FetchedPluginDirectoryPayload) action.getPayload());
                break;
        }
    }

    public List<PluginModel> getSitePlugins(SiteModel site) {
        return PluginSqlUtils.getPlugins(site);
    }

    public PluginModel getSitePluginByName(SiteModel site, String name) {
        return PluginSqlUtils.getPluginByName(site, name);
    }

    public PluginInfoModel getPluginInfoBySlug(String slug) {
        return PluginSqlUtils.getPluginInfoBySlug(slug);
    }

    private void fetchSitePlugins(SiteModel site) {
        if (site.isUsingWpComRestApi() && site.isJetpackConnected()) {
            mPluginRestClient.fetchSitePlugins(site);
        } else {
            FetchSitePluginsError error = new FetchSitePluginsError(FetchPluginsErrorType.NOT_AVAILABLE);
            FetchedSitePluginsPayload payload = new FetchedSitePluginsPayload(error);
            fetchedSitePlugins(payload);
        }
    }

    private void updateSitePlugin(UpdateSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.updatePlugin(payload.site, payload.plugin);
        } else {
            UpdateSitePluginError error = new UpdateSitePluginError(UpdateSitePluginErrorType.NOT_AVAILABLE);
            UpdatedSitePluginPayload errorPayload = new UpdatedSitePluginPayload(payload.site, error);
            updatedSitePlugin(errorPayload);
        }
    }

    private void fetchPluginInfo(String plugin) {
        mPluginWPOrgClient.fetchPluginInfo(plugin);
    }

    private void fetchPluginDirectory(PluginWPOrgClient.FetchPluginDirectoryPayload payload) {
        mPluginWPOrgClient.fetchPluginDirectory(payload);
    }

    private void fetchedSitePlugins(FetchedSitePluginsPayload payload) {
        OnSitePluginsChanged event = new OnSitePluginsChanged(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            PluginSqlUtils.insertOrReplacePlugins(payload.site, payload.plugins);
        }
        emitChange(event);
    }

    private void fetchedPluginInfo(FetchedPluginInfoPayload payload) {
        OnPluginInfoChanged event = new OnPluginInfoChanged();
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.pluginInfo = payload.pluginInfo;
            PluginSqlUtils.insertOrUpdatePluginInfo(payload.pluginInfo);
        }
        emitChange(event);
    }

    private void fetchedPluginDirectory(FetchedPluginDirectoryPayload payload) {
        OnPluginDirectoryChanged onPluginDirectoryChanged = new OnPluginDirectoryChanged();

        if (payload.isError()) {
            onPluginDirectoryChanged.error = payload.error;
        } else {
            onPluginDirectoryChanged.page = payload.page;
            for (PluginInfoModel pluginInfo : payload.plugins) {
                PluginSqlUtils.insertOrUpdatePluginInfo(pluginInfo);
            }
        }

        emitChange(onPluginDirectoryChanged);
    }

    private void updatedSitePlugin(UpdatedSitePluginPayload payload) {
        OnSitePluginChanged event = new OnSitePluginChanged(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            payload.plugin.setLocalSiteId(payload.site.getId());
            event.plugin = payload.plugin;
            PluginSqlUtils.insertOrUpdatePlugin(payload.site, payload.plugin);
        }
        emitChange(event);
    }
}
