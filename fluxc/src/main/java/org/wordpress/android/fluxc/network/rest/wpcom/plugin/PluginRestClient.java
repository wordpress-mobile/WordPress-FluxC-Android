package org.wordpress.android.fluxc.network.rest.wpcom.plugin;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginWPComRestResponse.FetchPluginsResponse;
import org.wordpress.android.fluxc.store.PluginStore.FetchPluginsErrorType;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginErrorType;
import org.wordpress.android.fluxc.store.Store.OnChangedError;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PluginRestClient extends BaseWPComRestClient {
    // Payloads
    public static class UpdateSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public PluginModel plugin;

        public UpdateSitePluginPayload(@NonNull SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }
    }

    public static class FetchedSitePluginsPayload extends Payload<FetchSitePluginsError> {
        public SiteModel site;
        public List<PluginModel> plugins;
        public FetchSitePluginsError error;

        public FetchedSitePluginsPayload(FetchSitePluginsError error) {
            this.error = error;
        }

        public FetchedSitePluginsPayload(@NonNull SiteModel site, @NonNull List<PluginModel> plugins) {
            this.site = site;
            this.plugins = plugins;
        }
    }

    public static class UpdatedSitePluginPayload extends Payload<UpdateSitePluginError> {
        public SiteModel site;
        public PluginModel plugin;
        public UpdateSitePluginError error;

        public UpdatedSitePluginPayload(@NonNull SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }

        public UpdatedSitePluginPayload(@NonNull SiteModel site, UpdateSitePluginError error) {
            this.site = site;
            this.error = error;
        }
    }

    public static class FetchSitePluginsError implements OnChangedError {
        public FetchPluginsErrorType type;
        public String message;
        public FetchSitePluginsError(FetchPluginsErrorType type) {
            this(type, "");
        }

        FetchSitePluginsError(FetchPluginsErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class UpdateSitePluginError implements OnChangedError {
        public UpdateSitePluginErrorType type;
        public String message;

        public UpdateSitePluginError(UpdateSitePluginErrorType type) {
            this.type = type;
        }
    }

    @Inject
    public PluginRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                              AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchSitePlugins(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).plugins.getUrlV1_1();
        final WPComGsonRequest<FetchPluginsResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                FetchPluginsResponse.class,
                new Listener<FetchPluginsResponse>() {
                    @Override
                    public void onResponse(FetchPluginsResponse response) {
                        List<PluginModel> plugins = new ArrayList<>();
                        if (response.plugins != null) {
                            for (PluginWPComRestResponse pluginResponse : response.plugins) {
                                plugins.add(pluginModelFromResponse(site, pluginResponse));
                            }
                        }
                        mDispatcher.dispatch(PluginActionBuilder.newFetchedSitePluginsAction(
                                new FetchedSitePluginsPayload(site, plugins)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        FetchSitePluginsError fetchSitePluginsError
                                = new FetchSitePluginsError(FetchPluginsErrorType.GENERIC_ERROR);
                        if (networkError instanceof WPComGsonNetworkError) {
                            switch (((WPComGsonNetworkError) networkError).apiError) {
                                case "unauthorized":
                                    fetchSitePluginsError.type = FetchPluginsErrorType.UNAUTHORIZED;
                                    break;
                            }
                        }
                        fetchSitePluginsError.message = networkError.message;
                        FetchedSitePluginsPayload payload = new FetchedSitePluginsPayload(fetchSitePluginsError);
                        mDispatcher.dispatch(PluginActionBuilder.newFetchedSitePluginsAction(payload));
                    }
                }
        );
        add(request);
    }

    public void updatePlugin(@NonNull final SiteModel site, @NonNull final PluginModel plugin) {
        String name;
        try {
            // We need to encode plugin name otherwise names like "akismet/akismet" would fail
            name = URLEncoder.encode(plugin.getName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            name = plugin.getName();
        }
        String url = WPCOMREST.sites.site(site.getSiteId()).plugins.name(name).getUrlV1_1();
        Map<String, Object> params = paramsFromPluginModel(plugin);
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, params,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                        PluginModel pluginModel = pluginModelFromResponse(site, response);
                        mDispatcher.dispatch(PluginActionBuilder.newUpdatedSitePluginAction(
                                new UpdatedSitePluginPayload(site, pluginModel)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        UpdateSitePluginError updateSitePluginError
                                = new UpdateSitePluginError(UpdateSitePluginErrorType.GENERIC_ERROR);
                        if (networkError instanceof WPComGsonNetworkError) {
                            switch (((WPComGsonNetworkError) networkError).apiError) {
                                case "unauthorized":
                                    updateSitePluginError.type = UpdateSitePluginErrorType.UNAUTHORIZED;
                                    break;
                            }
                        }
                        updateSitePluginError.message = networkError.message;
                        UpdatedSitePluginPayload payload = new UpdatedSitePluginPayload(site,
                                updateSitePluginError);
                        mDispatcher.dispatch(PluginActionBuilder.newUpdatedSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }

    private PluginModel pluginModelFromResponse(SiteModel siteModel, PluginWPComRestResponse response) {
        PluginModel pluginModel = new PluginModel();
        pluginModel.setLocalSiteId(siteModel.getId());
        pluginModel.setName(response.id);
        pluginModel.setDisplayName(response.name);
        pluginModel.setAuthorName(response.author);
        pluginModel.setAuthorUrl(response.author_url);
        pluginModel.setDescription(response.description);
        pluginModel.setIsActive(response.active);
        pluginModel.setIsAutoUpdateEnabled(response.autoupdate);
        pluginModel.setPluginUrl(response.plugin_url);
        pluginModel.setSlug(response.slug);
        pluginModel.setVersion(response.version);
        return pluginModel;
    }

    private Map<String, Object> paramsFromPluginModel(PluginModel pluginModel) {
        Map<String, Object> params = new HashMap<>();
        params.put("active", pluginModel.isActive());
        params.put("autoupdate", pluginModel.isAutoUpdateEnabled());
        return params;
    }
}
