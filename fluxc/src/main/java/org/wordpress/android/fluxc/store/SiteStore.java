package org.wordpress.android.fluxc.store;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.SelectMapper;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.RequestPayload;
import org.wordpress.android.fluxc.ResponsePayload;
import org.wordpress.android.fluxc.action.SiteAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.DeleteSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.ExportSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.FetchWPComSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.IsWPComResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils.DuplicateSiteException;
import org.wordpress.android.fluxc.utils.SiteErrorUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * SQLite based only. There is no in memory copy of mapped data, everything is queried from the DB.
 */
@Singleton
public class SiteStore extends Store {
    // Payloads
    public static class UrlRequestPayload extends RequestPayload {
        public final String url;

        public UrlRequestPayload(String url) {
            this.url = url;
        }
    }

    public static class RefreshSitesXMLRPCPayload extends RequestPayload {
        public RefreshSitesXMLRPCPayload(String username, String password, String url) {
            this.username = username;
            this.password = password;
            this.url = url;
        }
        public String username;
        public String password;
        public String url;
    }

    public static class NewSitePayload extends RequestPayload {
        public String siteName;
        public String siteTitle;
        public String language;
        public SiteVisibility visibility;
        public boolean dryRun;
        public NewSitePayload(@NonNull String siteName, @NonNull String siteTitle, @NonNull String language,
                              SiteVisibility visibility, boolean dryRun) {
            this.siteName = siteName;
            this.siteTitle = siteTitle;
            this.language = language;
            this.visibility = visibility;
            this.dryRun = dryRun;
        }
    }

    public static class FetchedPostFormatsPayload extends ResponsePayload {
        public SiteModel site;
        public List<PostFormatModel> postFormats;
        public PostFormatsError error;
        public FetchedPostFormatsPayload(@NonNull RequestPayload requestPayload, @NonNull SiteModel site,
                                         @NonNull List<PostFormatModel> postFormats) {
            super(requestPayload);
            this.site = site;
            this.postFormats = postFormats;
        }
    }

    public static class FetchedUserRolesPayload extends ResponsePayload {
        public SiteModel site;
        public List<RoleModel> roles;
        public UserRolesError error;
        public FetchedUserRolesPayload(@NonNull RequestPayload requestPayload, @NonNull SiteModel site,
                                       @NonNull List<RoleModel> roles) {
            super(requestPayload);
            this.site = site;
            this.roles = roles;
        }
    }

    public static class SuggestDomainsPayload extends RequestPayload {
        public String query;
        public boolean includeWordpressCom;
        public boolean includeDotBlogSubdomain;
        public int quantity;
        public SuggestDomainsPayload(@NonNull String query, boolean includeWordpressCom,
                                     boolean includeDotBlogSubdomain, int quantity) {
            this.query = query;
            this.includeWordpressCom = includeWordpressCom;
            this.includeDotBlogSubdomain = includeDotBlogSubdomain;
            this.quantity = quantity;
        }
    }

    public static class SuggestDomainsResponsePayload extends RequestPayload {
        public String query;
        public List<DomainSuggestionResponse> suggestions;
        public SuggestDomainsResponsePayload(@NonNull String query, BaseNetworkError error) {
            this.query = query;
            this.error = error;
            this.suggestions = new ArrayList<>();
        }

        public SuggestDomainsResponsePayload(@NonNull String query, ArrayList<DomainSuggestionResponse> suggestions) {
            this.query = query;
            this.suggestions = suggestions;
        }
    }

    public static class ConnectSiteInfoPayload extends ResponsePayload {
        public String url;
        public boolean exists;
        public boolean isWordPress;
        public boolean hasJetpack;
        public boolean isJetpackActive;
        public boolean isJetpackConnected;
        public boolean isWPCom;
        public SiteError error;

        public ConnectSiteInfoPayload(@NonNull UrlRequestPayload urlRequestPayload, SiteError error) {
            super(urlRequestPayload);
            this.url = urlRequestPayload.url;
            this.error = error;
        }

        public String description() {
            return String.format("url: %s, e: %b, wp: %b, jp: %b, wpcom: %b",
                    url, exists, isWordPress, hasJetpack, isWPCom);
        }
    }

    public static class RemoveAllSitesPayload extends RequestPayload {}

    public static class RemoveWpcomAndJetpackSitesPayload extends RequestPayload {}

    public static class SiteRequestPayload extends RequestPayload {
        public final SiteModel site;

        public SiteRequestPayload(SiteModel site) {
            this.site = site;
        }

        public SiteRequestPayload(long forwardRequestId, SiteModel site) {
            super(forwardRequestId);
            this.site = site;
        }
    }

    public static class SiteResponsePayload extends ResponsePayload {
        public final SiteModel site;

        public SiteResponsePayload(RequestPayload requestPayload, SiteModel site) {
            super(requestPayload);
            this.site = site;
        }
    }

    public static class FetchAllSitesPayload extends RequestPayload {}

    public static class SitesRequestPayload extends RequestPayload {
        public final SitesModel sites;

        public SitesRequestPayload(SitesModel sites) {
            this.sites = sites;
        }
    }

    public static class SitesResponsePayload extends ResponsePayload {
        public final SitesModel sites;

        public SitesResponsePayload(RequestPayload requestPayload, SitesModel sites) {
            super(requestPayload);
            this.sites = sites;
        }
    }

    public static class SiteError implements OnChangedError {
        public SiteErrorType type;
        public String message;

        public SiteError(SiteErrorType type) {
            this(type, "");
        }

        public SiteError(SiteErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class PostFormatsError implements OnChangedError {
        public PostFormatsErrorType type;
        public String message;

        public PostFormatsError(PostFormatsErrorType type) {
            this(type, "");
        }

        public PostFormatsError(PostFormatsErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class UserRolesError implements OnChangedError {
        public UserRolesErrorType type;
        public String message;

        public UserRolesError(UserRolesErrorType type) {
            this(type, "");
        }

        UserRolesError(UserRolesErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class NewSiteError implements OnChangedError {
        public NewSiteErrorType type;
        public String message;
        public NewSiteError(NewSiteErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class DeleteSiteError implements OnChangedError {
        public DeleteSiteErrorType type;
        public String message;
        public DeleteSiteError(String errorType, @NonNull String message) {
            this.type = DeleteSiteErrorType.fromString(errorType);
            this.message = message;
        }
        public DeleteSiteError(DeleteSiteErrorType errorType) {
            this.type = errorType;
            this.message = "";
        }
    }

    public static class ExportSiteError implements OnChangedError {
        public ExportSiteErrorType type;

        public ExportSiteError(ExportSiteErrorType type) {
            this.type = type;
        }
    }

    // OnChanged Events
    public static class OnSiteChanged extends OnChanged<SiteError> {
        public int rowsAffected;
        public OnSiteChanged(RequestPayload requestPayload, int rowsAffected) {
            super(requestPayload);
            this.rowsAffected = rowsAffected;
        }
    }

    public static class OnSiteRemoved extends OnChanged<SiteError> {
        public int mRowsAffected;
        public OnSiteRemoved(RequestPayload requestPayload, int rowsAffected) {
            super(requestPayload);
            mRowsAffected = rowsAffected;
        }
    }

    public static class OnAllSitesRemoved extends OnChanged<SiteError> {
        public int mRowsAffected;
        public OnAllSitesRemoved(RequestPayload requestPayload, int rowsAffected) {
            super(requestPayload);
            mRowsAffected = rowsAffected;
        }
    }

    public static class OnNewSiteCreated extends OnChanged<NewSiteError> {
        public boolean dryRun;
        public long newSiteRemoteId;
        public OnNewSiteCreated(RequestPayload requestPayload) {
            super(requestPayload);
        }
    }

    public static class OnSiteDeleted extends OnChanged<DeleteSiteError> {
        public OnSiteDeleted(RequestPayload requestPayload, DeleteSiteError error) {
            super(requestPayload);
            this.error = error;
        }
    }

    public static class OnSiteExported extends OnChanged<ExportSiteError> {
        public OnSiteExported(RequestPayload requestPayload) {
            super(requestPayload);
        }
    }

    public static class OnPostFormatsChanged extends OnChanged<PostFormatsError> {
        public SiteModel site;
        public OnPostFormatsChanged(RequestPayload requestPayload, SiteModel site) {
            super(requestPayload);
            this.site = site;
        }
    }

    public static class OnUserRolesChanged extends OnChanged<UserRolesError> {
        public SiteModel site;
        public OnUserRolesChanged(RequestPayload requestPayload, SiteModel site) {
            super(requestPayload);
            this.site = site;
        }
    }

    public static class OnURLChecked extends OnChanged<SiteError> {
        public String url;
        public boolean isWPCom;
        public OnURLChecked(RequestPayload requestPayload, @NonNull String url) {
            super(requestPayload);
            this.url = url;
        }
    }

    public static class OnConnectSiteInfoChecked extends OnChanged<SiteError> {
        public ConnectSiteInfoPayload info;
        public OnConnectSiteInfoChecked(@NonNull ConnectSiteInfoPayload info) {
            super(info.getRequestPayload());
            this.info = info;
        }
    }

    public static class OnWPComSiteFetched extends OnChanged<SiteError> {
        public String checkedUrl;
        public SiteModel site;
        public OnWPComSiteFetched(RequestPayload requestPayload, String checkedUrl, @NonNull SiteModel site) {
            super(requestPayload);
            this.checkedUrl = checkedUrl;
            this.site = site;
        }
    }

    public static class SuggestDomainError implements OnChangedError {
        public SuggestDomainErrorType type;
        public String message;
        public SuggestDomainError(@NonNull String apiErrorType, @NonNull String message) {
            this.type = SuggestDomainErrorType.fromString(apiErrorType);
            this.message = message;
        }
    }

    public static class OnSuggestedDomains extends OnChanged<SuggestDomainError> {
        public String query;
        public List<DomainSuggestionResponse> suggestions;
        public OnSuggestedDomains(RequestPayload requestPayload, @NonNull String query,
                                  @NonNull List<DomainSuggestionResponse> suggestions) {
            super(requestPayload);
            this.query = query;
            this.suggestions = suggestions;
        }
    }

    public static class UpdateSitesResult {
        public int rowsAffected = 0;
        public boolean duplicateSiteFound = false;
    }

    public enum SiteErrorType {
        INVALID_SITE,
        UNKNOWN_SITE,
        DUPLICATE_SITE,
        INVALID_RESPONSE,
        UNAUTHORIZED,
        GENERIC_ERROR
    }

    public enum SuggestDomainErrorType {
        EMPTY_QUERY,
        INVALID_MINIMUM_QUANTITY,
        INVALID_MAXIMUM_QUANTITY,
        GENERIC_ERROR;

        public static SuggestDomainErrorType fromString(final String string) {
            if (!TextUtils.isEmpty(string)) {
                for (SuggestDomainErrorType v : SuggestDomainErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum PostFormatsErrorType {
        INVALID_SITE,
        INVALID_RESPONSE,
        GENERIC_ERROR;
    }

    public enum UserRolesErrorType {
        GENERIC_ERROR
    }

    public enum DeleteSiteErrorType {
        INVALID_SITE,
        UNAUTHORIZED, // user don't have permission to delete
        AUTHORIZATION_REQUIRED, // missing access token
        GENERIC_ERROR;

        public static DeleteSiteErrorType fromString(final String string) {
            if (!TextUtils.isEmpty(string)) {
                if (string.equals("unauthorized")) {
                    return UNAUTHORIZED;
                } else if (string.equals("authorization_required")) {
                    return AUTHORIZATION_REQUIRED;
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum ExportSiteErrorType {
        INVALID_SITE,
        GENERIC_ERROR
    }

    // Enums
    public enum NewSiteErrorType {
        SITE_NAME_REQUIRED,
        SITE_NAME_NOT_ALLOWED,
        SITE_NAME_MUST_BE_AT_LEAST_FOUR_CHARACTERS,
        SITE_NAME_MUST_BE_LESS_THAN_SIXTY_FOUR_CHARACTERS,
        SITE_NAME_CONTAINS_INVALID_CHARACTERS,
        SITE_NAME_CANT_BE_USED,
        SITE_NAME_ONLY_LOWERCASE_LETTERS_AND_NUMBERS,
        SITE_NAME_MUST_INCLUDE_LETTERS,
        SITE_NAME_EXISTS,
        SITE_NAME_RESERVED,
        SITE_NAME_RESERVED_BUT_MAY_BE_AVAILABLE,
        SITE_NAME_INVALID,
        SITE_TITLE_INVALID,
        GENERIC_ERROR;

        // SiteStore semantics prefers SITE over BLOG but errors reported from the API use BLOG
        // these are used to convert API errors to the appropriate enum value in fromString
        private static final String BLOG = "BLOG";
        private static final String SITE = "SITE";

        public static NewSiteErrorType fromString(final String string) {
            if (!TextUtils.isEmpty(string)) {
                String siteString = string.toUpperCase(Locale.US).replace(BLOG, SITE);
                for (NewSiteErrorType v : NewSiteErrorType.values()) {
                    if (siteString.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum SiteVisibility {
        PRIVATE(-1),
        BLOCK_SEARCH_ENGINE(0),
        PUBLIC(1);

        private final int mValue;

        SiteVisibility(int value) {
            this.mValue = value;
        }

        public int value() {
            return mValue;
        }
    }

    private SiteRestClient mSiteRestClient;
    private SiteXMLRPCClient mSiteXMLRPCClient;

    @Inject
    public SiteStore(Dispatcher dispatcher, SiteRestClient siteRestClient, SiteXMLRPCClient siteXMLRPCClient) {
        super(dispatcher);
        mSiteRestClient = siteRestClient;
        mSiteXMLRPCClient = siteXMLRPCClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, "SiteStore onRegister");
    }

    /**
     * Returns all sites in the store as a {@link SiteModel} list.
     */
    public List<SiteModel> getSites() {
        return WellSql.select(SiteModel.class).getAsModel();
    }

    /**
     * Returns all sites in the store as a {@link Cursor}.
     */
    public Cursor getSitesCursor() {
        return WellSql.select(SiteModel.class).getAsCursor();
    }

    /**
     * Returns the number of sites of any kind in the store.
     */
    public int getSitesCount() {
        return getSitesCursor().getCount();
    }

    /**
     * Checks whether the store contains any sites of any kind.
     */
    public boolean hasSite() {
        return getSitesCount() != 0;
    }

    /**
     * Obtains the site with the given (local) id and returns it as a {@link SiteModel}.
     */
    public SiteModel getSiteByLocalId(int id) {
        List<SiteModel> result = SiteSqlUtils.getSitesWith(SiteModelTable.ID, id).getAsModel();
        if (result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    /**
     * Checks whether the store contains a site matching the given (local) id.
     */
    public boolean hasSiteWithLocalId(int id) {
        return SiteSqlUtils.getSitesWith(SiteModelTable.ID, id).getAsCursor().getCount() > 0;
    }

    /**
     * Returns all .COM sites in the store.
     */
    public List<SiteModel> getWPComSites() {
        return SiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel();
    }

    /**
     * Returns sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected via WPCom REST API).
     */
    public List<SiteModel> getSitesAccessedViaWPComRest() {
        return SiteSqlUtils.getSitesAccessedViaWPComRest().getAsModel();
    }

    /**
     * Returns the number of sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected
     * via WPCom REST API).
     */
    public int getSitesAccessedViaWPComRestCount() {
        return SiteSqlUtils.getSitesAccessedViaWPComRest().getAsCursor().getCount();
    }

    /**
     * Checks whether the store contains at least one site accessed via WPCom REST API (WPCom sites or Jetpack
     * sites connected via WPCom REST API).
     */
    public boolean hasSitesAccessedViaWPComRest() {
        return getSitesAccessedViaWPComRestCount() != 0;
    }

    /**
     * Returns the number of .COM sites in the store.
     */
    public int getWPComSitesCount() {
        return SiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsCursor().getCount();
    }

    /**
     * Returns sites with a name or url matching the search string.
     */
    @NonNull
    public List<SiteModel> getSitesByNameOrUrlMatching(@NonNull String searchString) {
        return SiteSqlUtils.getSitesByNameOrUrlMatching(searchString);
    }

    /**
     * Returns sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected via WPCom REST API) with a
     * name or url matching the search string.
     */
    @NonNull
    public List<SiteModel> getSitesAccessedViaWPComRestByNameOrUrlMatching(@NonNull String searchString) {
        return SiteSqlUtils.getSitesAccessedViaWPComRestByNameOrUrlMatching(searchString);
    }

    /**
     * Checks whether the store contains at least one .COM site.
     */
    public boolean hasWPComSite() {
        return getWPComSitesCount() != 0;
    }

    /**
     * Returns sites accessed via XMLRPC (self-hosted sites or Jetpack sites accessed via XMLRPC).
     */
    public List<SiteModel> getSitesAccessedViaXMLRPC() {
        return SiteSqlUtils.getSitesAccessedViaXMLRPC().getAsModel();
    }

    /**
     * Returns the number of sites accessed via XMLRPC (self-hosted sites or Jetpack sites accessed via XMLRPC).
     */
    public int getSitesAccessedViaXMLRPCCount() {
        return SiteSqlUtils.getSitesAccessedViaXMLRPC().getAsCursor().getCount();
    }

    /**
     * Checks whether the store contains at least one site accessed via XMLRPC (self-hosted sites or
     * Jetpack sites accessed via XMLRPC).
     */
    public boolean hasSiteAccessedViaXMLRPC() {
        return getSitesAccessedViaXMLRPCCount() != 0;
    }

    /**
     * Returns all visible sites as {@link SiteModel}s. All self-hosted sites over XML-RPC are visible by default.
     */
    public List<SiteModel> getVisibleSites() {
        return SiteSqlUtils.getSitesWith(SiteModelTable.IS_VISIBLE, true).getAsModel();
    }

    /**
     * Returns the number of visible sites. All self-hosted sites over XML-RPC are visible by default.
     */
    public int getVisibleSitesCount() {
        return SiteSqlUtils.getSitesWith(SiteModelTable.IS_VISIBLE, true).getAsCursor().getCount();
    }

    /**
     * Returns all visible .COM sites as {@link SiteModel}s.
     */
    public List<SiteModel> getVisibleSitesAccessedViaWPCom() {
        return SiteSqlUtils.getVisibleSitesAccessedViaWPCom().getAsModel();
    }

    /**
     * Returns the number of visible .COM sites.
     */
    public int getVisibleSitesAccessedViaWPComCount() {
        return SiteSqlUtils.getVisibleSitesAccessedViaWPCom().getAsCursor().getCount();
    }

    /**
     * Checks whether the .COM site with the given (local) id is visible.
     */
    public boolean isWPComSiteVisibleByLocalId(int id) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .equals(SiteModelTable.IS_WPCOM, true)
                .equals(SiteModelTable.IS_VISIBLE, true)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    /**
     * Given a (remote) site id, returns the corresponding (local) id.
     */
    public int getLocalIdForRemoteSiteId(long siteId) {
        List<SiteModel> sites = WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, siteId)
                .or()
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, siteId)
                .endGroup().endWhere()
                .getAsModel(new SelectMapper<SiteModel>() {
                    @Override
                    public SiteModel convert(Cursor cursor) {
                        SiteModel siteModel = new SiteModel();
                        siteModel.setId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.ID)));
                        return siteModel;
                    }
                });
        if (sites.size() > 0) {
            return sites.get(0).getId();
        }
        return 0;
    }

    /**
     * Given a (remote) self-hosted site id and XML-RPC url, returns the corresponding (local) id.
     */
    public int getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(long selfHostedSiteId, String xmlRpcUrl) {
        List<SiteModel> sites = WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, selfHostedSiteId)
                .equals(SiteModelTable.XMLRPC_URL, xmlRpcUrl)
                .endGroup().endWhere()
                .getAsModel(new SelectMapper<SiteModel>() {
                    @Override
                    public SiteModel convert(Cursor cursor) {
                        SiteModel siteModel = new SiteModel();
                        siteModel.setId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.ID)));
                        return siteModel;
                    }
                });
        if (sites.size() > 0) {
            return sites.get(0).getId();
        }
        return 0;
    }

    /**
     * Given a (local) id, returns the (remote) site id. Searches first for .COM and Jetpack, then looks for self-hosted
     * sites.
     */
    public long getSiteIdForLocalId(int id) {
        List<SiteModel> result = WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .endGroup().endWhere()
                .getAsModel(new SelectMapper<SiteModel>() {
                    @Override
                    public SiteModel convert(Cursor cursor) {
                        SiteModel siteModel = new SiteModel();
                        siteModel.setSiteId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.SITE_ID)));
                        siteModel.setSelfHostedSiteId(cursor.getLong(
                                cursor.getColumnIndex(SiteModelTable.SELF_HOSTED_SITE_ID)));
                        return siteModel;
                    }
                });
        if (result.isEmpty()) {
            return 0;
        }

        if (result.get(0).getSiteId() > 0) {
            return result.get(0).getSiteId();
        } else {
            return result.get(0).getSelfHostedSiteId();
        }
    }

    /**
     * Given a .COM site ID (either a .COM site id, or the .COM id of a Jetpack site), returns the site as a
     * {@link SiteModel}.
     */
    public SiteModel getSiteBySiteId(long siteId) {
        if (siteId == 0) {
            return null;
        }

        List<SiteModel> sites = SiteSqlUtils.getSitesWith(SiteModelTable.SITE_ID, siteId).getAsModel();

        if (sites.isEmpty()) {
            return null;
        } else {
            return sites.get(0);
        }
    }

    public List<PostFormatModel> getPostFormats(SiteModel site) {
        return SiteSqlUtils.getPostFormats(site);
    }

    public List<RoleModel> getUserRoles(SiteModel site) {
        return SiteSqlUtils.getUserRoles(site);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof SiteAction)) {
            return;
        }

        switch ((SiteAction) actionType) {
            case FETCH_SITE:
                fetchSite((SiteRequestPayload) action.getPayload());
                break;
            case FETCH_SITES:
                mSiteRestClient.fetchSites((FetchAllSitesPayload) action.getPayload());
                break;
            case FETCHED_SITES:
                handleFetchedSitesWPComRest((SitesResponsePayload) action.getPayload());
                break;
            case FETCH_SITES_XML_RPC:
                fetchSitesXmlRpc((RefreshSitesXMLRPCPayload) action.getPayload());
                break;
            case FETCHED_SITES_XML_RPC:
                updateSites(((SitesResponsePayload) action.getPayload()).getRequestPayload(),
                        ((SitesResponsePayload) action.getPayload()).sites);
                break;
            case UPDATE_SITE:
                updateSite((SiteRequestPayload) action.getPayload());
                break;
            case UPDATE_SITES:
                updateSites((SitesRequestPayload) action.getPayload(), ((SitesRequestPayload) action.getPayload()).sites);
                break;
            case DELETE_SITE:
                deleteSite((SiteRequestPayload) action.getPayload());
                break;
            case DELETED_SITE:
                handleDeletedSite((DeleteSiteResponsePayload) action.getPayload());
                break;
            case EXPORT_SITE:
                exportSite((SiteRequestPayload) action.getPayload());
                break;
            case EXPORTED_SITE:
                handleExportedSite((ExportSiteResponsePayload) action.getPayload());
                break;
            case REMOVE_SITE:
                removeSite((SiteRequestPayload) action.getPayload());
                break;
            case REMOVE_ALL_SITES:
                removeAllSites((RemoveAllSitesPayload) action.getPayload());
                break;
            case REMOVE_WPCOM_AND_JETPACK_SITES:
                removeWPComAndJetpackSites((RemoveWpcomAndJetpackSitesPayload) action.getPayload());
                break;
            case SHOW_SITES:
                toggleSitesVisibility((SitesRequestPayload) action.getPayload(), true);
                break;
            case HIDE_SITES:
                toggleSitesVisibility((SitesRequestPayload) action.getPayload(), false);
                break;
            case CREATE_NEW_SITE:
                createNewSite((NewSitePayload) action.getPayload());
                break;
            case CREATED_NEW_SITE:
                handleCreateNewSiteCompleted((NewSiteResponsePayload) action.getPayload());
                break;
            case FETCH_POST_FORMATS:
                fetchPostFormats((SiteRequestPayload) action.getPayload());
                break;
            case FETCHED_POST_FORMATS:
                updatePostFormats((FetchedPostFormatsPayload) action.getPayload());
                break;
            case FETCH_USER_ROLES:
                fetchUserRoles((SiteRequestPayload) action.getPayload());
                break;
            case FETCHED_USER_ROLES:
                updateUserRoles((FetchedUserRolesPayload) action.getPayload());
                break;
            case FETCH_CONNECT_SITE_INFO:
                fetchConnectSiteInfo((UrlRequestPayload) action.getPayload());
                break;
            case FETCHED_CONNECT_SITE_INFO:
                handleFetchedConnectSiteInfo((ConnectSiteInfoPayload) action.getPayload());
                break;
            case FETCH_WPCOM_SITE_BY_URL:
                fetchWPComSiteByUrl((UrlRequestPayload) action.getPayload());
                break;
            case FETCHED_WPCOM_SITE_BY_URL:
                handleFetchedWPComSiteByUrl((FetchWPComSiteResponsePayload) action.getPayload());
                break;
            case IS_WPCOM_URL:
                checkUrlIsWPCom((UrlRequestPayload) action.getPayload());
                break;
            case CHECKED_IS_WPCOM_URL:
                handleCheckedIsWPComUrl((IsWPComResponsePayload) action.getPayload());
                break;
            case SUGGEST_DOMAINS:
                suggestDomains((SuggestDomainsPayload) action.getPayload());
                break;
            case SUGGESTED_DOMAINS:
                handleSuggestedDomains((SuggestDomainsResponsePayload) action.getPayload());
                break;
        }
    }

    private void fetchSite(SiteRequestPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mSiteRestClient.fetchSite(payload);
        } else {
            mSiteXMLRPCClient.fetchSite(payload);
        }
    }

    private void fetchSitesXmlRpc(RefreshSitesXMLRPCPayload payload) {
        mSiteXMLRPCClient.fetchSites(payload);
    }

    private void updateSite(@NonNull SiteRequestPayload payload) {
        OnSiteChanged event = new OnSiteChanged(payload, 0);
        if (payload.site.isError()) {
            // TODO: what kind of error could we get here?
            event.error = SiteErrorUtils.genericToSiteError(payload.site.error);
        } else {
            try {
                event.rowsAffected = SiteSqlUtils.insertOrUpdateSite(payload.site);
            } catch (DuplicateSiteException e) {
                event.error = new SiteError(SiteErrorType.DUPLICATE_SITE);
            }
        }
        emitChange(event);
    }

    private void updateSites(@NonNull RequestPayload payload, @NonNull SitesModel sites) {
        OnSiteChanged event = new OnSiteChanged(payload, 0);
        if (sites.isError()) {
            // TODO: what kind of error could we get here?
            event.error = SiteErrorUtils.genericToSiteError(sites.error);
        } else {
            UpdateSitesResult res = createOrUpdateSites(sites);
            event.rowsAffected = res.rowsAffected;
            if (res.duplicateSiteFound) {
                event.error = new SiteError(SiteErrorType.DUPLICATE_SITE);
            }
        }
        emitChange(event);
    }

    private void handleFetchedSitesWPComRest(SitesResponsePayload payload) {
        OnSiteChanged event = new OnSiteChanged(payload.getRequestPayload(), 0);
        if (payload.sites.isError()) {
            // TODO: what kind of error could we get here?
            event.error = SiteErrorUtils.genericToSiteError(payload.sites.error);
        } else {
            UpdateSitesResult res = createOrUpdateSites(payload.sites);
            event.rowsAffected = res.rowsAffected;
            if (res.duplicateSiteFound) {
                event.error = new SiteError(SiteErrorType.DUPLICATE_SITE);
            }
            SiteSqlUtils.removeWPComRestSitesAbsentFromList(payload.sites.getSites());
        }
        emitChange(event);
    }

    private UpdateSitesResult createOrUpdateSites(SitesModel sites) {
        UpdateSitesResult result = new UpdateSitesResult();
        for (SiteModel site : sites.getSites()) {
            try {
                result.rowsAffected += SiteSqlUtils.insertOrUpdateSite(site);
            } catch (DuplicateSiteException caughtException) {
                result.duplicateSiteFound = true;
            }
        }
        return result;
    }

    private void deleteSite(@NonNull SiteRequestPayload payload) {
        // Not available for Jetpack sites
        if (!payload.site.isWPCom()) {
            OnSiteDeleted event = new OnSiteDeleted(payload, new DeleteSiteError(DeleteSiteErrorType.INVALID_SITE));
            emitChange(event);
            return;
        }
        mSiteRestClient.deleteSite(payload, payload.site);
    }

    private void handleDeletedSite(DeleteSiteResponsePayload payload) {
        OnSiteDeleted event = new OnSiteDeleted(payload.getRequestPayload(), payload.error);
        if (!payload.isError()) {
            SiteSqlUtils.deleteSite(payload.site);
        }
        emitChange(event);
    }

    private void exportSite(SiteRequestPayload payload) {
        // Not available for Jetpack sites
        if (!payload.site.isWPCom()) {
            OnSiteExported event = new OnSiteExported(payload);
            event.error = new ExportSiteError(ExportSiteErrorType.INVALID_SITE);
            emitChange(event);
            return;
        }
        mSiteRestClient.exportSite(payload, payload.site);
    }

    private void handleExportedSite(ExportSiteResponsePayload payload) {
        OnSiteExported event = new OnSiteExported(payload.getRequestPayload());
        if (payload.isError()) {
            // TODO: what kind of error could we get here?
            event.error = new ExportSiteError(ExportSiteErrorType.GENERIC_ERROR);
        }
        emitChange(event);
    }

    private void removeSite(SiteRequestPayload payload) {
        int rowsAffected = SiteSqlUtils.deleteSite(payload.site);
        emitChange(new OnSiteRemoved(payload, rowsAffected));
    }

    private void removeAllSites(RemoveAllSitesPayload payload) {
        int rowsAffected = SiteSqlUtils.deleteAllSites();
        OnAllSitesRemoved event = new OnAllSitesRemoved(payload, rowsAffected);
        emitChange(event);
    }

    private void removeWPComAndJetpackSites(RemoveWpcomAndJetpackSitesPayload payload) {
        // Logging out of WP.com. Drop all WP.com sites, and all Jetpack sites that were fetched over the WP.com
        // REST API only (they don't have a .org site id)
        List<SiteModel> wpcomAndJetpackSites = SiteSqlUtils.getSitesAccessedViaWPComRest().getAsModel();
        int rowsAffected = removeSites(wpcomAndJetpackSites);
        emitChange(new OnSiteRemoved(payload, rowsAffected));
    }

    private int toggleSitesVisibility(SitesRequestPayload payload, boolean visible) {
        int rowsAffected = 0;
        for (SiteModel site : payload.sites.getSites()) {
            rowsAffected += SiteSqlUtils.setSiteVisibility(site, visible);
        }
        return rowsAffected;
    }

    private void createNewSite(NewSitePayload payload) {
        mSiteRestClient.newSite(payload);
    }

    private void handleCreateNewSiteCompleted(NewSiteResponsePayload payload) {
        OnNewSiteCreated onNewSiteCreated = new OnNewSiteCreated(payload.getRequestPayload());
        onNewSiteCreated.error = payload.error;
        onNewSiteCreated.dryRun = payload.dryRun;
        onNewSiteCreated.newSiteRemoteId = payload.newSiteRemoteId;
        emitChange(onNewSiteCreated);
    }

    private void fetchPostFormats(SiteRequestPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mSiteRestClient.fetchPostFormats(payload);
        } else {
            mSiteXMLRPCClient.fetchPostFormats(payload);
        }
    }

    private void updatePostFormats(FetchedPostFormatsPayload payload) {
        OnPostFormatsChanged event = new OnPostFormatsChanged(payload.getRequestPayload(), payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            SiteSqlUtils.insertOrReplacePostFormats(payload.site, payload.postFormats);
        }
        emitChange(event);
    }

    private void fetchUserRoles(SiteRequestPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mSiteRestClient.fetchUserRoles(payload, payload.site);
        }
    }

    private void updateUserRoles(FetchedUserRolesPayload payload) {
        OnUserRolesChanged event = new OnUserRolesChanged(payload.getRequestPayload(), payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            SiteSqlUtils.insertOrReplaceUserRoles(payload.site, payload.roles);
        }
        emitChange(event);
    }

    private int removeSites(List<SiteModel> sites) {
        int rowsAffected = 0;
        for (SiteModel site : sites) {
            rowsAffected += SiteSqlUtils.deleteSite(site);
        }
        return rowsAffected;
    }

    private void fetchConnectSiteInfo(UrlRequestPayload payload) {
        mSiteRestClient.fetchConnectSiteInfo(payload);
    }

    private void handleFetchedConnectSiteInfo(ConnectSiteInfoPayload payload) {
        OnConnectSiteInfoChecked event = new OnConnectSiteInfoChecked(payload);
        event.error = payload.error;
        emitChange(event);
    }

    private void fetchWPComSiteByUrl(UrlRequestPayload payload) {
        mSiteRestClient.fetchWPComSiteByUrl(payload);
    }

    private void handleFetchedWPComSiteByUrl(FetchWPComSiteResponsePayload payload) {
        OnWPComSiteFetched event = new OnWPComSiteFetched(payload.getRequestPayload(), payload.checkedUrl, payload.site);
        event.error = payload.error;
        emitChange(event);
    }

    private void checkUrlIsWPCom(UrlRequestPayload payload) {
        mSiteRestClient.checkUrlIsWPCom(payload);
    }

    private void handleCheckedIsWPComUrl(IsWPComResponsePayload payload) {
        OnURLChecked event = new OnURLChecked(payload.getRequestPayload(), payload.url);
        if (payload.isError()) {
            // Return invalid site for all errors (this endpoint seems a bit drunk).
            // Client likely needs to know if there was an error or not.
            event.error = new SiteError(SiteErrorType.INVALID_SITE);
        }
        event.isWPCom = payload.isWPCom;
        emitChange(event);
    }

    private void suggestDomains(SuggestDomainsPayload payload) {
        mSiteRestClient.suggestDomains(payload);
    }

    private void handleSuggestedDomains(SuggestDomainsResponsePayload payload) {
        OnSuggestedDomains event = new OnSuggestedDomains(payload, payload.query, payload.suggestions);
        if (payload.isError()) {
            if (payload.error instanceof WPComGsonRequest.WPComGsonNetworkError) {
                event.error = new SuggestDomainError(((WPComGsonNetworkError) payload.error).apiError,
                        payload.error.message);
            } else {
                event.error = new SuggestDomainError("", payload.error.message);
            }
        }
        emitChange(event);
    }
}
