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
import org.wordpress.android.fluxc.network.wporg.plugin.FetchPluginInfoResponse.BrowsePluginResponse;
import org.wordpress.android.fluxc.store.PluginStore.FetchPluginInfoError;
import org.wordpress.android.fluxc.store.PluginStore.FetchPluginInfoErrorType;
import org.wordpress.android.fluxc.store.PluginStore.FetchedPluginInfoPayload;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PluginWPOrgClient extends BaseWPOrgAPIClient {
    private final Dispatcher mDispatcher;

    public static class BrowsePluginPayload extends Payload {
        public int page;

        public BrowsePluginPayload() {
            page = 1;
        }

        private Map<String, String> getParams() {
            Map<String, String> params = new HashMap<>();
            params.put("page", String.valueOf(page));
            return params;
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

    public void fetchPlugins(BrowsePluginPayload payload) {
        String url = "https://api.wordpress.org/plugins/info/1.1/?action=query_plugins";
        final WPOrgAPIGsonRequest<BrowsePluginResponse> request =
                new WPOrgAPIGsonRequest<>(Method.GET, url, payload.getParams(), null, BrowsePluginResponse.class,
                        new Listener<BrowsePluginResponse>() {
                            @Override
                            public void onResponse(BrowsePluginResponse response) {
                            }
                        },
                        new BaseErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                            }
                        }
                );
        add(request);
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
}
