package org.wordpress.android.fluxc.network.rest.wpcom.site;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.RequestPayload;
import org.wordpress.android.fluxc.ResponsePayload;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteWPComRestResponse.SitesResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.site.UserRoleWPComRestResponse.UserRolesResponse;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;
import org.wordpress.android.fluxc.store.SiteStore.DeleteSiteError;
import org.wordpress.android.fluxc.store.SiteStore.FetchAllSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedUserRolesPayload;
import org.wordpress.android.fluxc.store.SiteStore.NewSiteError;
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType;
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload;
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsError;
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SiteError;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SiteResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteRequestPayload;
import org.wordpress.android.fluxc.store.SiteStore.SitesResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.UrlRequestPayload;
import org.wordpress.android.fluxc.store.SiteStore.UserRolesError;
import org.wordpress.android.fluxc.store.SiteStore.UserRolesErrorType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SiteRestClient extends BaseWPComRestClient {
    public static final int NEW_SITE_TIMEOUT_MS = 90000;

    private final AppSecrets mAppSecrets;

    public static class NewSiteResponsePayload extends ResponsePayload {
        public NewSiteResponsePayload(RequestPayload requestPayload) {
            super(requestPayload);
        }
        public long newSiteRemoteId;
        public NewSiteError error;
        public boolean dryRun;
    }

    public static class DeleteSiteResponsePayload extends ResponsePayload {
        public DeleteSiteResponsePayload(RequestPayload requestPayload) {
            super(requestPayload);
        }
        public SiteModel site;
        public DeleteSiteError error;
    }

    public static class ExportSiteResponsePayload extends ResponsePayload {
        public ExportSiteResponsePayload(RequestPayload requestPayload) {
            super(requestPayload);
        }
    }

    public static class IsWPComResponsePayload extends ResponsePayload {
        public IsWPComResponsePayload(RequestPayload requestPayload) {
            super(requestPayload);
        }

        public String url;
        public boolean isWPCom;
    }

    public static class FetchWPComSiteResponsePayload extends ResponsePayload {
        public FetchWPComSiteResponsePayload(RequestPayload requestPayload) {
            super(requestPayload);
        }
        public String checkedUrl;
        public SiteModel site;
        public SiteError error;
    }

    @Inject
    public SiteRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue, AppSecrets appSecrets,
                          AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mAppSecrets = appSecrets;
    }

    public void fetchSites(final FetchAllSitesPayload fetchAllSitesPayload) {
        String url = WPCOMREST.me.sites.getUrlV1_1();
        final WPComGsonRequest<SitesResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                SitesResponse.class,
                new Listener<SitesResponse>() {
                    @Override
                    public void onResponse(SitesResponse response) {
                        if (response != null) {
                            List<SiteModel> siteArray = new ArrayList<>();

                            for (SiteWPComRestResponse siteResponse : response.sites) {
                                siteArray.add(siteResponseToSiteModel(siteResponse));
                            }
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesAction(
                                    new SitesResponsePayload(fetchAllSitesPayload, new SitesModel(siteArray))));
                        } else {
                            AppLog.e(T.API, "Received empty response to /me/sites/");
                            SitesModel payload = new SitesModel(Collections.<SiteModel>emptyList());
                            payload.error = new BaseNetworkError(GenericErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesAction(
                                    new SitesResponsePayload(fetchAllSitesPayload, new SitesModel())));
                        }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        SitesModel payload = new SitesModel(Collections.<SiteModel>emptyList());
                        payload.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesAction(
                                new SitesResponsePayload(fetchAllSitesPayload, payload)));
                    }
                }
        );
        add(fetchAllSitesPayload, request);
    }

    public void fetchSite(final SiteRequestPayload siteRequestPayload) {
        String url = WPCOMREST.sites.getUrlV1_1() + siteRequestPayload.site.getSiteId();
        final WPComGsonRequest<SiteWPComRestResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                SiteWPComRestResponse.class,
                new Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        if (response != null) {
                            SiteModel site = siteResponseToSiteModel(response);
                            mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(
                                    new SiteRequestPayload(siteRequestPayload.getRequestId(), site)));
                        } else {
                            AppLog.e(T.API, "Received empty response to /sites/$site/ for " +
                                    siteRequestPayload.site.getUrl());
                            SiteModel payload = new SiteModel();
                            payload.error = new BaseNetworkError(GenericErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(
                                    new SiteRequestPayload(siteRequestPayload.getRequestId(), payload)));
                        }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        SiteModel payload = new SiteModel();
                        payload.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(
                                new SiteRequestPayload(siteRequestPayload.getRequestId(), payload)));
                    }
                }
        );
        add(siteRequestPayload, request);
    }

    public void newSite(@NonNull final NewSitePayload newSitePayload) {
        String url = WPCOMREST.sites.new_.getUrlV1();
        Map<String, Object> body = new HashMap<>();
        body.put("blog_name", newSitePayload.siteName);
        body.put("blog_title", newSitePayload.siteTitle);
        body.put("lang_id", newSitePayload.language);
        body.put("public", newSitePayload.visibility.toString());
        body.put("validate", newSitePayload.dryRun ? "1" : "0");
        body.put("client_id", mAppSecrets.getAppId());
        body.put("client_secret", mAppSecrets.getAppSecret());

        WPComGsonRequest<NewSiteResponse> request = WPComGsonRequest.buildPostRequest(url, body,
                NewSiteResponse.class,
                new Listener<NewSiteResponse>() {
                    @Override
                    public void onResponse(NewSiteResponse response) {
                        NewSiteResponsePayload payload = new NewSiteResponsePayload(newSitePayload);
                        payload.dryRun = newSitePayload.dryRun;
                        long siteId = 0;
                        if (response.blog_details != null) {
                            try {
                                siteId = Long.valueOf(response.blog_details.blogid);
                            } catch (NumberFormatException e) {
                                // No op: In dry run mode, returned newSiteRemoteId is "Array"
                            }
                        }
                        payload.newSiteRemoteId = siteId;
                        mDispatcher.dispatch(SiteActionBuilder.newCreatedNewSiteAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        NewSiteResponsePayload payload = volleyErrorToAccountResponsePayload(newSitePayload,
                                error.volleyError);
                        payload.dryRun = newSitePayload.dryRun;
                        mDispatcher.dispatch(SiteActionBuilder.newCreatedNewSiteAction(payload));
                    }
                }
        );

        // Disable retries and increase timeout for site creation (it can sometimes take a long time to complete)
        request.setRetryPolicy(new DefaultRetryPolicy(NEW_SITE_TIMEOUT_MS, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        add(newSitePayload, request);
    }

    public void fetchPostFormats(@NonNull final SiteRequestPayload siteRequestPayload) {
        String url = WPCOMREST.sites.site(siteRequestPayload.site.getSiteId()).post_formats.getUrlV1_1();
        final WPComGsonRequest<PostFormatsResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                PostFormatsResponse.class,
                new Listener<PostFormatsResponse>() {
                    @Override
                    public void onResponse(PostFormatsResponse response) {
                        List<PostFormatModel> postFormats = new ArrayList<>();
                        if (response.formats != null) {
                            for (String key : response.formats.keySet()) {
                                PostFormatModel postFormat = new PostFormatModel();
                                postFormat.setSlug(key);
                                postFormat.setDisplayName(response.formats.get(key));
                                postFormats.add(postFormat);
                            }
                        }
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(new
                                FetchedPostFormatsPayload(siteRequestPayload, siteRequestPayload.site, postFormats)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        FetchedPostFormatsPayload payload = new FetchedPostFormatsPayload(siteRequestPayload,
                                siteRequestPayload.site, Collections.<PostFormatModel>emptyList());
                        // TODO: what other kind of error could we get here?
                        payload.error = new PostFormatsError(PostFormatsErrorType.GENERIC_ERROR);
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(payload));
                    }
                }
        );
        add(siteRequestPayload, request);
    }

    public void fetchUserRoles(@NonNull final RequestPayload requestPayload, final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).roles.getUrlV1_1();
        final WPComGsonRequest<UserRolesResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                UserRolesResponse.class,
                new Listener<UserRolesResponse>() {
                    @Override
                    public void onResponse(UserRolesResponse response) {
                        List<RoleModel> roleArray = new ArrayList<>();
                        for (UserRoleWPComRestResponse roleResponse : response.roles) {
                            RoleModel roleModel = new RoleModel();
                            roleModel.setName(roleResponse.name);
                            roleModel.setDisplayName(roleResponse.display_name);
                            roleArray.add(roleModel);
                        }
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedUserRolesAction(new
                                FetchedUserRolesPayload(requestPayload, site, roleArray)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        FetchedUserRolesPayload payload = new FetchedUserRolesPayload(requestPayload, site,
                                Collections.<RoleModel>emptyList());
                        // TODO: what other kind of error could we get here?
                        payload.error = new UserRolesError(UserRolesErrorType.GENERIC_ERROR);
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedUserRolesAction(payload));
                    }
                }
        );
        add(requestPayload, request);
    }

    public void deleteSite(final RequestPayload requestPayload, final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).delete.getUrlV1_1();
        WPComGsonRequest<SiteWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                SiteWPComRestResponse.class,
                new Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        DeleteSiteResponsePayload payload = new DeleteSiteResponsePayload(requestPayload);
                        payload.site = site;
                        mDispatcher.dispatch(SiteActionBuilder.newDeletedSiteAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        DeleteSiteResponsePayload payload = new DeleteSiteResponsePayload(requestPayload);
                        WPComGsonNetworkError networkError = ((WPComGsonNetworkError) error);
                        payload.error = new DeleteSiteError(networkError.apiError, networkError.message);
                        payload.site = site;
                        mDispatcher.dispatch(SiteActionBuilder.newDeletedSiteAction(payload));
                    }
                }
        );
        add(requestPayload, request);
    }

    public void exportSite(@NonNull final RequestPayload requestPayload, final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).exports.start.getUrlV1_1();
        final WPComGsonRequest<ExportSiteResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                ExportSiteResponse.class,
                new Listener<ExportSiteResponse>() {
                    @Override
                    public void onResponse(ExportSiteResponse response) {
                        ExportSiteResponsePayload payload = new ExportSiteResponsePayload(requestPayload);
                        mDispatcher.dispatch(SiteActionBuilder.newExportedSiteAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        ExportSiteResponsePayload payload = new ExportSiteResponsePayload(requestPayload);
                        payload.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newExportedSiteAction(payload));
                    }
                }
        );
        add(requestPayload, request);
    }

    //
    // Unauthenticated network calls
    //

    public void fetchConnectSiteInfo(@NonNull final UrlRequestPayload urlRequestPayload) {
        // Get a proper URI to reliably retrieve the scheme.
        URI uri;
        try {
            uri = URI.create(UrlUtils.addUrlSchemeIfNeeded(urlRequestPayload.url, false));
        } catch (IllegalArgumentException e) {
            SiteError siteError = new SiteError(SiteErrorType.INVALID_SITE);
            ConnectSiteInfoPayload payload = new ConnectSiteInfoPayload(urlRequestPayload, siteError);
            mDispatcher.dispatch(SiteActionBuilder.newFetchedConnectSiteInfoAction(payload));
            return;
        }

        // Sanitize and encode the Url for the API call.
        String sanitizedURL = UrlUtils.removeScheme(urlRequestPayload.url);
        sanitizedURL = sanitizedURL.replace("/", "::");

        // Make the call.
        String url = WPCOMREST.connect.site_info.protocol(uri.getScheme()).address(sanitizedURL).getUrlV1_1();
        final WPComGsonRequest<ConnectSiteInfoResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                ConnectSiteInfoResponse.class,
                new Listener<ConnectSiteInfoResponse>() {
                    @Override
                    public void onResponse(ConnectSiteInfoResponse response) {
                        ConnectSiteInfoPayload info = connectSiteInfoFromResponse(urlRequestPayload, response);
                        info.url = urlRequestPayload.url;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedConnectSiteInfoAction(info));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        SiteError siteError = new SiteError(SiteErrorType.INVALID_SITE);
                        ConnectSiteInfoPayload info = new ConnectSiteInfoPayload(urlRequestPayload, siteError);
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedConnectSiteInfoAction(info));
                    }
                }
        );
        addUnauthedRequest(urlRequestPayload, request);
    }

    public void fetchWPComSiteByUrl(@NonNull final UrlRequestPayload urlRequestPayload) {
        String sanitizedUrl;
        try {
            URI uri = URI.create(UrlUtils.addUrlSchemeIfNeeded(urlRequestPayload.url, false));
            sanitizedUrl = URLEncoder.encode(UrlUtils.removeScheme(uri.toString()), "UTF-8");
        } catch (IllegalArgumentException e) {
            FetchWPComSiteResponsePayload payload = new FetchWPComSiteResponsePayload(urlRequestPayload);
            payload.checkedUrl = urlRequestPayload.url;
            payload.error = new SiteError(SiteErrorType.INVALID_SITE);
            mDispatcher.dispatch(SiteActionBuilder.newFetchedWpcomSiteByUrlAction(payload));
            return;
        } catch (UnsupportedEncodingException e) {
            // This should be impossible (it means an Android device without UTF-8 support)
            throw new IllegalStateException(e);
        }

        String requestUrl = WPCOMREST.sites.siteUrl(sanitizedUrl).getUrlV1_1();

        final WPComGsonRequest<SiteWPComRestResponse> request = WPComGsonRequest.buildGetRequest(requestUrl, null,
                SiteWPComRestResponse.class,
                new Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        FetchWPComSiteResponsePayload payload = new FetchWPComSiteResponsePayload(urlRequestPayload);
                        payload.checkedUrl = urlRequestPayload.url;
                        payload.site = siteResponseToSiteModel(response);
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedWpcomSiteByUrlAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        FetchWPComSiteResponsePayload payload = new FetchWPComSiteResponsePayload(urlRequestPayload);
                        payload.checkedUrl = urlRequestPayload.url;

                        SiteErrorType siteErrorType = SiteErrorType.GENERIC_ERROR;
                        if (error instanceof WPComGsonNetworkError) {
                            switch (((WPComGsonNetworkError) error).apiError) {
                                case "unauthorized":
                                    siteErrorType = SiteErrorType.UNAUTHORIZED;
                                    break;
                                case "unknown_blog":
                                    siteErrorType = SiteErrorType.UNKNOWN_SITE;
                                    break;
                            }
                        }
                        payload.error = new SiteError(siteErrorType);

                        mDispatcher.dispatch(SiteActionBuilder.newFetchedWpcomSiteByUrlAction(payload));
                    }
                }
        );
        addUnauthedRequest(urlRequestPayload, request);
    }

    public void checkUrlIsWPCom(@NonNull final UrlRequestPayload urlRequestPayload) {
        String url = WPCOMREST.sites.getUrlV1_1() + urlRequestPayload.url;
        final WPComGsonRequest<SiteWPComRestResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                SiteWPComRestResponse.class,
                new Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        IsWPComResponsePayload payload = new IsWPComResponsePayload(urlRequestPayload);
                        payload.url = urlRequestPayload.url;
                        payload.isWPCom = true;
                        mDispatcher.dispatch(SiteActionBuilder.newCheckedIsWpcomUrlAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        IsWPComResponsePayload payload = new IsWPComResponsePayload(urlRequestPayload);
                        payload.url = urlRequestPayload.url;
                        // "unauthorized" and "unknown_blog" errors expected if the site is not accessible via
                        // the WPCom REST API.
                        if (error instanceof WPComGsonNetworkError
                                && ("unauthorized".equals(((WPComGsonNetworkError) error).apiError)
                                || "unknown_blog".equals(((WPComGsonNetworkError) error).apiError))) {
                            payload.isWPCom = false;
                        } else {
                            payload.error = error;
                        }
                        mDispatcher.dispatch(SiteActionBuilder.newCheckedIsWpcomUrlAction(payload));
                    }
                }
        );
        addUnauthedRequest(urlRequestPayload, request);
    }

    public void suggestDomains(@NonNull final SuggestDomainsPayload suggestDomainsPayload) {
        String url = WPCOMREST.domains.suggestions.getUrlV1_1();
        Map<String, String> params = new HashMap<>(4);
        params.put("query", suggestDomainsPayload.query);
        // ugly trick to bypass checkstyle and its dot com rule
        params.put("include_wordpressdot" + "com", String.valueOf(suggestDomainsPayload.includeWordpressCom));
        params.put("include_dotblogsubdomain", String.valueOf(suggestDomainsPayload.includeDotBlogSubdomain));
        params.put("quantity", String.valueOf(suggestDomainsPayload.quantity));
        final WPComGsonRequest<ArrayList<DomainSuggestionResponse>> request =
                WPComGsonRequest.buildGetRequest(url, params,
                        new TypeToken<ArrayList<DomainSuggestionResponse>>(){}.getType(),
                new Listener<ArrayList<DomainSuggestionResponse>>() {
                    @Override
                    public void onResponse(ArrayList<DomainSuggestionResponse> response) {
                        SuggestDomainsResponsePayload payload =
                                new SuggestDomainsResponsePayload(suggestDomainsPayload.query, response);
                        mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        SuggestDomainsResponsePayload payload =
                                new SuggestDomainsResponsePayload(suggestDomainsPayload.query, error);
                        mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload));
                    }
                }
        );
        addUnauthedRequest(suggestDomainsPayload, request);
    }

    private SiteModel siteResponseToSiteModel(SiteWPComRestResponse from) {
        SiteModel site = new SiteModel();
        site.setSiteId(from.ID);
        site.setUrl(from.URL);
        site.setName(StringEscapeUtils.unescapeHtml4(from.name));
        site.setDescription(StringEscapeUtils.unescapeHtml4(from.description));
        site.setIsJetpackConnected(from.jetpack);
        site.setIsJetpackInstalled(from.jetpack);
        site.setIsVisible(from.visible);
        site.setIsPrivate(from.is_private);
        // Depending of user's role, options could be "hidden", for instance an "Author" can't read site options.
        if (from.options != null) {
            site.setIsFeaturedImageSupported(from.options.featured_images_enabled);
            site.setIsVideoPressSupported(from.options.videopress_enabled);
            site.setIsAutomatedTransfer(from.options.is_automated_transfer);
            site.setAdminUrl(from.options.admin_url);
            site.setLoginUrl(from.options.login_url);
            site.setTimezone(from.options.gmt_offset);
            site.setFrameNonce(from.options.frame_nonce);
            site.setUnmappedUrl(from.options.unmapped_url);

            try {
                site.setMaxUploadSize(Long.valueOf(from.options.max_upload_size));
            } catch (NumberFormatException e) {
                // Do nothing - the value probably wasn't set ('false'), but we don't want to overwrite any existing
                // value we stored earlier, as /me/sites/ and /sites/$site/ can return different responses for this
            }

            // Set the memory limit for media uploads on the site. Normally, this is just WP_MAX_MEMORY_LIMIT,
            // but it's possible for a site to have its php memory_limit > WP_MAX_MEMORY_LIMIT, and have
            // WP_MEMORY_LIMIT == memory_limit, in which WP_MEMORY_LIMIT reflects the real limit for media uploads.
            long wpMemoryLimit = StringUtils.stringToLong(from.options.wp_memory_limit);
            long wpMaxMemoryLimit = StringUtils.stringToLong(from.options.wp_max_memory_limit);
            if (wpMemoryLimit > 0 || wpMaxMemoryLimit > 0) {
                // Only update the value if we received one from the server - otherwise, the original value was
                // probably not set ('false'), but we don't want to overwrite any existing value we stored earlier,
                // as /me/sites/ and /sites/$site/ can return different responses for this
                site.setMemoryLimit(Math.max(wpMemoryLimit, wpMaxMemoryLimit));
            }
        }
        if (from.plan != null) {
            try {
                site.setPlanId(Long.valueOf(from.plan.product_id));
            } catch (NumberFormatException e) {
                // VIP sites return a String plan ID ('vip') rather than a number
                if (from.plan.product_id.equals("vip")) {
                    site.setPlanId(SiteModel.VIP_PLAN_ID);
                }
            }
            site.setPlanShortName(from.plan.product_name_short);
            site.setHasFreePlan(from.plan.is_free);
        }
        if (from.capabilities != null) {
            site.setHasCapabilityEditPages(from.capabilities.edit_pages);
            site.setHasCapabilityEditPosts(from.capabilities.edit_posts);
            site.setHasCapabilityEditOthersPosts(from.capabilities.edit_others_posts);
            site.setHasCapabilityEditOthersPages(from.capabilities.edit_others_pages);
            site.setHasCapabilityDeletePosts(from.capabilities.delete_posts);
            site.setHasCapabilityDeleteOthersPosts(from.capabilities.delete_others_posts);
            site.setHasCapabilityEditThemeOptions(from.capabilities.edit_theme_options);
            site.setHasCapabilityEditUsers(from.capabilities.edit_users);
            site.setHasCapabilityListUsers(from.capabilities.list_users);
            site.setHasCapabilityManageCategories(from.capabilities.manage_categories);
            site.setHasCapabilityManageOptions(from.capabilities.manage_options);
            site.setHasCapabilityActivateWordads(from.capabilities.activate_wordads);
            site.setHasCapabilityPromoteUsers(from.capabilities.promote_users);
            site.setHasCapabilityPublishPosts(from.capabilities.publish_posts);
            site.setHasCapabilityUploadFiles(from.capabilities.upload_files);
            site.setHasCapabilityDeleteUser(from.capabilities.delete_user);
            site.setHasCapabilityRemoveUsers(from.capabilities.remove_users);
            site.setHasCapabilityViewStats(from.capabilities.view_stats);
        }
        if (from.icon != null) {
            site.setIconUrl(from.icon.img);
        }
        if (from.meta != null) {
            if (from.meta.links != null) {
                site.setXmlRpcUrl(from.meta.links.xmlrpc);
            }
        }
        // Only set the isWPCom flag for "pure" WPCom sites
        if (!from.jetpack) {
            site.setIsWPCom(true);
        }
        site.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        return site;
    }

    private NewSiteResponsePayload volleyErrorToAccountResponsePayload(RequestPayload requestPayload, VolleyError error) {
        NewSiteResponsePayload payload = new NewSiteResponsePayload(requestPayload);
        payload.error = new NewSiteError(NewSiteErrorType.GENERIC_ERROR, "");
        if (error.networkResponse != null && error.networkResponse.data != null) {
            String jsonString = new String(error.networkResponse.data);
            try {
                JSONObject errorObj = new JSONObject(jsonString);
                payload.error.type = NewSiteErrorType.fromString((String) errorObj.get("error"));
                payload.error.message = (String) errorObj.get("message");
            } catch (JSONException e) {
                // Do nothing (keep default error)
            }
        }
        return payload;
    }

    private ConnectSiteInfoPayload connectSiteInfoFromResponse(@NonNull UrlRequestPayload urlRequestPayload,
                                                               ConnectSiteInfoResponse response) {
        ConnectSiteInfoPayload info = new ConnectSiteInfoPayload(urlRequestPayload, null);
        info.url = urlRequestPayload.url;
        info.exists = response.exists;
        info.hasJetpack = response.hasJetpack;
        info.isJetpackActive = response.isJetpackActive;
        info.isJetpackConnected = response.isJetpackConnected;
        info.isWordPress = response.isWordPress;
        // CHECKSTYLE IGNORE RegexpSingleline
        info.isWPCom = response.isWordPressDotCom;
        // CHECKSTYLE END IGNORE RegexpSingleline
        return info;
    }
}
