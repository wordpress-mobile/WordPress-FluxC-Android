package org.wordpress.android.stores.network.rest.wpcom.site;

import android.support.annotation.NonNull;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.Payload;
import org.wordpress.android.stores.generated.SiteActionBuilder;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.model.SitesModel;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.stores.network.rest.wpcom.WPCOMREST;
import org.wordpress.android.stores.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.stores.network.rest.wpcom.account.NewAccountResponse;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.stores.network.rest.wpcom.site.SiteWPComRestResponse.SitesResponse;
import org.wordpress.android.stores.store.SiteStore.NewSiteError;
import org.wordpress.android.stores.store.SiteStore.SiteVisibility;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SiteRestClient extends BaseWPComRestClient {
    private final AppSecrets mAppSecrets;

    public static class NewSiteResponsePayload implements Payload {
        public NewSiteResponsePayload() {
        }
        public NewSiteError errorType;
        public String errorMessage;
        public boolean isError;
        public boolean dryRun;
    }

    @Inject
    public SiteRestClient(Dispatcher dispatcher, RequestQueue requestQueue, AppSecrets appSecrets,
                          AccessToken accessToken, UserAgent userAgent) {
        super(dispatcher, requestQueue, accessToken, userAgent);
        mAppSecrets = appSecrets;
    }

    public void pullSites() {
        String url = WPCOMREST.ME_SITES.getUrlV1_1();
        final WPComGsonRequest<SitesResponse> request = new WPComGsonRequest<>(Method.GET,
                url, null, SitesResponse.class,
                new Listener<SitesResponse>() {
                    @Override
                    public void onResponse(SitesResponse response) {
                        SitesModel sites = new SitesModel();
                        for (SiteWPComRestResponse siteResponse : response.sites) {
                            sites.add(siteResponseToSiteModel(siteResponse));
                        }
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSitesAction(sites));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.API, "Volley error", error);
                        // TODO: Error, dispatch network error
                    }
                }
        );
        add(request);
    }

    public void pullSite(final SiteModel site) {
        String url = WPCOMREST.SITES.getUrlV1_1() + site.getSiteId();
        final WPComGsonRequest<SiteWPComRestResponse> request = new WPComGsonRequest<>(Method.GET,
                url, null, SiteWPComRestResponse.class,
                new Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        SiteModel site = siteResponseToSiteModel(response);
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.API, "Volley error", error);
                        // TODO: Error, dispatch network error
                    }
                }
        );
        add(request);
    }

    public void newSite(@NonNull String siteName, @NonNull String siteTitle, @NonNull String language,
                        @NonNull SiteVisibility visibility, final boolean dryRun) {
        String url = WPCOMREST.SITES_NEW.getUrlV1();
        Map<String, String> params = new HashMap<>();
        params.put("blog_name", siteName);
        params.put("blog_title", siteTitle);
        params.put("lang_id", language);
        params.put("public", visibility.toString());
        params.put("validate", dryRun ? "1" : "0");
        params.put("client_id", mAppSecrets.getAppId());
        params.put("client_secret", mAppSecrets.getAppSecret());
        add(new WPComGsonRequest<>(Method.POST, url, params, NewAccountResponse.class,
                new Listener<NewAccountResponse>() {
                    @Override
                    public void onResponse(NewAccountResponse response) {
                        NewSiteResponsePayload payload = new NewSiteResponsePayload();
                        payload.isError = false;
                        payload.dryRun = dryRun;
                        mDispatcher.dispatch(SiteActionBuilder.newCreatedNewSiteAction(payload));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.API, new String(error.networkResponse.data));
                        NewSiteResponsePayload payload = volleyErrorToAccountResponsePayload(error);
                        payload.dryRun = dryRun;
                        mDispatcher.dispatch(SiteActionBuilder.newCreatedNewSiteAction(payload));
                    }
                }
        ));
    }

    private SiteModel siteResponseToSiteModel(SiteWPComRestResponse from) {
        SiteModel site = new SiteModel();
        site.setSiteId(from.ID);
        site.setUrl(from.URL);
        site.setName(from.name);
        site.setDescription(from.description);
        site.setIsJetpack(from.jetpack);
        site.setIsVisible(from.visible);
        // Depending of user's role, options could be "hidden", for instance an "Author" can't read blog options.
        if (from.options != null) {
            site.setIsFeaturedImageSupported(from.options.featured_images_enabled);
            site.setIsVideoPressSupported(from.options.videopress_enabled);
            site.setAdminUrl(from.options.admin_url);
            site.setLoginUrl(from.options.login_url);
            site.setTimezone(from.options.timezone);
        }
        if (from.plan != null) {
            site.setPlanId(from.plan.product_id);
            site.setPlanShortName(from.plan.product_name_short);
        }
        if (from.capabilities != null) {
            site.setIsCapabilityEditPages(from.capabilities.edit_pages);
            site.setIsCapabilityEditPosts(from.capabilities.edit_posts);
            site.setIsCapabilityEditOthersPosts(from.capabilities.edit_others_posts);
            site.setIsCapabilityEditOthersPages(from.capabilities.edit_others_pages);
            site.setIsCapabilityDeletePosts(from.capabilities.delete_posts);
            site.setIsCapabilityDeleteOthersPosts(from.capabilities.delete_others_posts);
            site.setIsCapabilityEditThemeOptions(from.capabilities.edit_theme_options);
            site.setIsCapabilityEditUsers(from.capabilities.edit_users);
            site.setIsCapabilityListUsers(from.capabilities.list_users);
            site.setIsCapabilityManageCategories(from.capabilities.manage_categories);
            site.setIsCapabilityManageOptions(from.capabilities.manage_options);
            site.setIsCapabilityActivateWordads(from.capabilities.activate_wordads);
            site.setIsCapabilityPromoteUsers(from.capabilities.promote_users);
            site.setIsCapabilityPublishPosts(from.capabilities.publish_posts);
            site.setIsCapabilityUploadFiles(from.capabilities.upload_files);
            site.setIsCapabilityDeleteUser(from.capabilities.delete_user);
            site.setIsCapabilityRemoveUsers(from.capabilities.remove_users);
            site.setIsCapabilityViewStats(from.capabilities.view_stats);
        }
        site.setIsWPCom(true);
        return site;
    }

    private NewSiteResponsePayload volleyErrorToAccountResponsePayload(VolleyError error) {
        NewSiteResponsePayload payload = new NewSiteResponsePayload();
        payload.isError = true;
        payload.errorType = NewSiteError.GENERIC_ERROR;
        if (error.networkResponse != null && error.networkResponse.data != null) {
            String jsonString = new String(error.networkResponse.data);
            try {
                JSONObject errorObj = new JSONObject(jsonString);
                payload.errorType = NewSiteError.fromString((String) errorObj.get("error"));
                payload.errorMessage = (String) errorObj.get("message");
            } catch (JSONException e) {
                // Do nothing (keep default error)
            }
        }
        return payload;
    }
}
