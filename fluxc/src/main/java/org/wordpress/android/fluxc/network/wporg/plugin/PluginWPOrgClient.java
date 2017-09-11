package org.wordpress.android.fluxc.network.wporg.plugin;

import android.support.annotation.NonNull;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPORGAPI;
import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.wporg.BaseWPOrgAPIClient;
import org.wordpress.android.fluxc.network.wporg.WPOrgAPIGsonRequest;
import org.wordpress.android.fluxc.network.wporg.plugin.FetchPluginInfoResponse.FetchPluginDirectoryResponse;
import org.wordpress.android.fluxc.store.PluginStore.FetchPluginInfoErrorType;
import org.wordpress.android.fluxc.store.Store.OnChangedError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PluginWPOrgClient extends BaseWPOrgAPIClient {
    private final Dispatcher mDispatcher;

    public static class FetchedPluginInfoPayload extends Payload {
        public PluginInfoModel pluginInfo;
        public FetchPluginInfoError error;

        FetchedPluginInfoPayload(FetchPluginInfoError error) {
            this.error = error;
        }

        FetchedPluginInfoPayload(PluginInfoModel pluginInfo) {
            this.pluginInfo = pluginInfo;
        }
    }

    public static class FetchPluginDirectoryPayload extends Payload {
        public int page;
        public int pageSize;

        public FetchPluginDirectoryPayload() {
            page = 1;
            pageSize = 30;
        }
    }

    public static class FetchedPluginDirectoryPayload extends Payload {
        public int page;
        public List<PluginInfoModel> plugins;
        public FetchPluginInfoError error;

        FetchedPluginDirectoryPayload(FetchPluginInfoError error) {
            this.error = error;
        }

        FetchedPluginDirectoryPayload(List<PluginInfoModel> plugins, int page) {
            this.plugins = plugins;
            this.page = page;
        }
    }

    public static class FetchPluginInfoError implements OnChangedError {
        public FetchPluginInfoErrorType type;

        public FetchPluginInfoError(FetchPluginInfoErrorType type) {
            this.type = type;
        }
    }

    @Inject
    public PluginWPOrgClient(Dispatcher dispatcher, RequestQueue requestQueue, UserAgent userAgent) {
        super(dispatcher, requestQueue, userAgent);
        mDispatcher = dispatcher;
    }

    public void fetchPluginInfo(String plugin) {
        String url = WPORGAPI.plugins.info.version("1.0").slug(plugin).getUrl();
        Map<String, String> params = new HashMap<>();
        params.put("fields", "icons");
        final WPOrgAPIGsonRequest<FetchPluginInfoResponse> request =
                new WPOrgAPIGsonRequest<>(Method.GET, url, params, null, FetchPluginInfoResponse.class,
                        new Listener<FetchPluginInfoResponse>() {
                            @Override
                            public void onResponse(FetchPluginInfoResponse response) {
                                PluginInfoModel pluginInfoModel = pluginInfoModelFromResponse(response);
                                FetchedPluginInfoPayload payload = new FetchedPluginInfoPayload(pluginInfoModel);
                                mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginInfoAction(payload));
                            }
                        },
                        new BaseErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                                FetchPluginInfoError error = new FetchPluginInfoError(
                                        FetchPluginInfoErrorType.GENERIC_ERROR);
                                mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginInfoAction(
                                        new FetchedPluginInfoPayload(error)));
                            }
                        }
                );
        add(request);
    }

    public void fetchPluginDirectory(FetchPluginDirectoryPayload payload) {
        String url = WPORGAPI.plugins.info.version("1.1").getUrl();
        Map<String, String> params = getPluginDirectoryParams(payload);
        final WPOrgAPIGsonRequest<FetchPluginDirectoryResponse> request =
                new WPOrgAPIGsonRequest<>(Method.GET, url, params, null, FetchPluginDirectoryResponse.class,
                        new Listener<FetchPluginDirectoryResponse>() {
                            @Override
                            public void onResponse(FetchPluginDirectoryResponse response) {
                                FetchedPluginDirectoryPayload fetchedPluginDirectoryPayload =
                                        pluginDirectoryPayloadFromResponse(response);
                                mDispatcher.dispatch(PluginActionBuilder.
                                        newFetchedPluginDirectoryAction(fetchedPluginDirectoryPayload));
                            }
                        },
                        new BaseErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                                FetchPluginInfoError error = new FetchPluginInfoError(
                                        FetchPluginInfoErrorType.GENERIC_ERROR);
                                mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginDirectoryAction(
                                        new FetchedPluginDirectoryPayload(error)));
                            }
                        }
                );
        add(request);
    }

    private Map<String, String> getPluginDirectoryParams(FetchPluginDirectoryPayload payload) {
        Map<String, String> params = new HashMap<>();
        // This parameter is necessary for browse plugin actions
        params.put("action", "query_plugins");
        params.put("page", String.valueOf(payload.page));
        params.put("per_page", String.valueOf(payload.pageSize));
        params.put("fields", "icons");
        return params;
    }

    private PluginInfoModel pluginInfoModelFromResponse(FetchPluginInfoResponse response) {
        PluginInfoModel pluginInfo = new PluginInfoModel();
        pluginInfo.setName(response.name);
        pluginInfo.setRating(response.rating);
        pluginInfo.setSlug(response.slug);
        pluginInfo.setVersion(response.version);
        pluginInfo.setIcon(response.icon);
        return pluginInfo;
    }

    private FetchedPluginDirectoryPayload pluginDirectoryPayloadFromResponse(FetchPluginDirectoryResponse response) {
        List<PluginInfoModel> plugins = new ArrayList<>();
        for (FetchPluginInfoResponse pluginInfoResponse : response.plugins) {
            plugins.add(pluginInfoModelFromResponse(pluginInfoResponse));
        }
        return new FetchedPluginDirectoryPayload(plugins, response.info.page);
    }
}
