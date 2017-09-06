package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.RequestPayload;
import org.wordpress.android.fluxc.ResponsePayload;
import org.wordpress.android.fluxc.action.PluginAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.fluxc.store.SiteStore.SiteRequestPayload;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;

public class PluginStore extends Store {
    // Payloads
    public static class PluginInfoRequestPayload extends RequestPayload {
        public final String plugin;
        public PluginInfoRequestPayload(String plugin) {
            this.plugin = plugin;
        }
    }

    public static class UpdatePluginPayload extends RequestPayload {
        public SiteModel site;
        public PluginModel plugin;

        public UpdatePluginPayload(SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }
    }

    public static class FetchedPluginsPayload extends ResponsePayload {
        public SiteModel site;
        public List<PluginModel> plugins;
        public FetchPluginsError error;

        public FetchedPluginsPayload(@NonNull RequestPayload requestPayload, FetchPluginsError error) {
            super(requestPayload);
            this.error = error;
        }

        public FetchedPluginsPayload(@NonNull RequestPayload requestPayload, @NonNull SiteModel site,
                                     @NonNull List<PluginModel> plugins) {
            super(requestPayload);
            this.site = site;
            this.plugins = plugins;
        }
    }

    public static class FetchedPluginInfoPayload extends ResponsePayload {
        public PluginInfoModel pluginInfo;
        public FetchPluginInfoError error;

        public FetchedPluginInfoPayload(@NonNull RequestPayload requestPayload, FetchPluginInfoError error) {
            super(requestPayload);
            this.error = error;
        }

        public FetchedPluginInfoPayload(@NonNull RequestPayload requestPayload, PluginInfoModel pluginInfo) {
            super(requestPayload);
            this.pluginInfo = pluginInfo;
        }
    }

    public static class UpdatedPluginPayload extends ResponsePayload {
        public SiteModel site;
        public PluginModel plugin;
        public UpdatePluginError error;

        public UpdatedPluginPayload(@NonNull RequestPayload requestPayload, SiteModel site, PluginModel plugin) {
            super(requestPayload);
            this.site = site;
            this.plugin = plugin;
        }

        public UpdatedPluginPayload(@NonNull RequestPayload requestPayload, SiteModel site, UpdatePluginError error) {
            super(requestPayload);
            this.error = error;
        }
    }

    public static class FetchPluginsError implements OnChangedError {
        public FetchPluginsErrorType type;
        public String message;
        public FetchPluginsError(FetchPluginsErrorType type) {
            this(type, "");
        }

        FetchPluginsError(FetchPluginsErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class FetchPluginInfoError implements OnChangedError {
        public FetchPluginInfoErrorType type;

        public FetchPluginInfoError(FetchPluginInfoErrorType type) {
            this.type = type;
        }
    }

    public static class UpdatePluginError implements OnChangedError {
        public UpdatePluginErrorType type;
        public String message;

        public UpdatePluginError(UpdatePluginErrorType type) {
            this.type = type;
        }
    }

    public enum FetchPluginsErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        NOT_AVAILABLE // Return for non-jetpack sites
    }

    public enum FetchPluginInfoErrorType {
        GENERIC_ERROR
    }

    public enum UpdatePluginErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        NOT_AVAILABLE // Return for non-jetpack sites
    }

    public static class OnPluginsChanged extends OnChanged<FetchPluginsError> {
        public SiteModel site;
        public OnPluginsChanged(@NonNull RequestPayload requestPayload, SiteModel site) {
            super(requestPayload);
            this.site = site;
        }
    }

    public static class OnPluginInfoChanged extends OnChanged<FetchPluginInfoError> {
        public PluginInfoModel pluginInfo;
        public OnPluginInfoChanged(RequestPayload requestPayload) {
            super(requestPayload);
        }
    }

    public static class OnPluginChanged extends OnChanged<UpdatePluginError> {
        public SiteModel site;
        public PluginModel plugin;
        public OnPluginChanged(@NonNull RequestPayload requestPayload, SiteModel site) {
            super(requestPayload);
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
            case FETCH_PLUGINS:
                fetchPlugins((SiteRequestPayload) action.getPayload());
                break;
            case FETCH_PLUGIN_INFO:
                fetchPluginInfo((PluginInfoRequestPayload) action.getPayload());
                break;
            case UPDATE_PLUGIN:
                updatePlugin((UpdatePluginPayload) action.getPayload());
                break;
            case FETCHED_PLUGINS:
                fetchedPlugins((FetchedPluginsPayload) action.getPayload());
                break;
            case FETCHED_PLUGIN_INFO:
                fetchedPluginInfo((FetchedPluginInfoPayload) action.getPayload());
                break;
            case UPDATED_PLUGIN:
                updatedPlugin((UpdatedPluginPayload) action.getPayload());
                break;
        }
    }

    public List<PluginModel> getPlugins(SiteModel site) {
        return PluginSqlUtils.getPlugins(site);
    }

    public PluginModel getPluginByName(SiteModel site, String name) {
        return PluginSqlUtils.getPluginByName(site, name);
    }

    public PluginInfoModel getPluginInfoBySlug(String slug) {
        return PluginSqlUtils.getPluginInfoBySlug(slug);
    }

    private void fetchPlugins(SiteRequestPayload siteRequestPayload) {
        if (siteRequestPayload.site.isUsingWpComRestApi() && siteRequestPayload.site.isJetpackConnected()) {
            mPluginRestClient.fetchPlugins(siteRequestPayload, siteRequestPayload.site);
        } else {
            FetchPluginsError error = new FetchPluginsError(FetchPluginsErrorType.NOT_AVAILABLE);
            FetchedPluginsPayload payload = new FetchedPluginsPayload(siteRequestPayload, error);
            fetchedPlugins(payload);
        }
    }

    private void fetchPluginInfo(PluginInfoRequestPayload payload) {
        mPluginWPOrgClient.fetchPluginInfo(payload, payload.plugin);
    }

    private void updatePlugin(UpdatePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.updatePlugin(payload, payload.site, payload.plugin);
        } else {
            UpdatePluginError error = new UpdatePluginError(UpdatePluginErrorType.NOT_AVAILABLE);
            UpdatedPluginPayload errorPayload = new UpdatedPluginPayload(payload, payload.site, error);
            updatedPlugin(errorPayload);
        }
    }

    private void fetchedPlugins(FetchedPluginsPayload payload) {
        OnPluginsChanged event = new OnPluginsChanged(payload.getRequestPayload(), payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            PluginSqlUtils.insertOrReplacePlugins(payload.site, payload.plugins);
        }
        emitChange(event);
    }

    private void fetchedPluginInfo(FetchedPluginInfoPayload payload) {
        OnPluginInfoChanged event = new OnPluginInfoChanged(payload.getRequestPayload());
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.pluginInfo = payload.pluginInfo;
            PluginSqlUtils.insertOrUpdatePluginInfo(payload.pluginInfo);
        }
        emitChange(event);
    }

    private void updatedPlugin(UpdatedPluginPayload payload) {
        OnPluginChanged event = new OnPluginChanged(payload.getRequestPayload(), payload.site);
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
