package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.DeleteSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.ExportSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.FetchWPComSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.IsWPComResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchAllSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedUserRolesPayload;
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.fluxc.store.SiteStore.RemoveAllSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.RemoveWpcomAndJetpackSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteRequestPayload;
import org.wordpress.android.fluxc.store.SiteStore.SitesResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.SitesRequestPayload;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.UrlRequestPayload;

@ActionEnum
public enum SiteAction implements IAction {
    // Remote actions
    @Action(payloadType = SiteRequestPayload.class)
    FETCH_SITE,
    @Action(payloadType = FetchAllSitesPayload.class)
    FETCH_SITES,
    @Action(payloadType = RefreshSitesXMLRPCPayload.class)
    FETCH_SITES_XML_RPC,
    @Action(payloadType = NewSitePayload.class)
    CREATE_NEW_SITE,
    @Action(payloadType = SiteRequestPayload.class)
    FETCH_POST_FORMATS,
    @Action(payloadType = SiteRequestPayload.class)
    FETCH_USER_ROLES,
    @Action(payloadType = SiteRequestPayload.class)
    DELETE_SITE,
    @Action(payloadType = SiteRequestPayload.class)
    EXPORT_SITE,
    @Action(payloadType = UrlRequestPayload.class)
    IS_WPCOM_URL,
    @Action(payloadType = SuggestDomainsPayload.class)
    SUGGEST_DOMAINS,
    @Action(payloadType = UrlRequestPayload.class)
    FETCH_CONNECT_SITE_INFO,
    @Action(payloadType = UrlRequestPayload.class)
    FETCH_WPCOM_SITE_BY_URL,

    // Remote responses
    @Action(payloadType = SitesResponsePayload.class)
    FETCHED_SITES,
    @Action(payloadType = SitesResponsePayload.class)
    FETCHED_SITES_XML_RPC,
    @Action(payloadType = NewSiteResponsePayload.class)
    CREATED_NEW_SITE,
    @Action(payloadType = FetchedPostFormatsPayload.class)
    FETCHED_POST_FORMATS,
    @Action(payloadType = FetchedUserRolesPayload.class)
    FETCHED_USER_ROLES,
    @Action(payloadType = DeleteSiteResponsePayload.class)
    DELETED_SITE,
    @Action(payloadType = ExportSiteResponsePayload.class)
    EXPORTED_SITE,
    @Action(payloadType = ConnectSiteInfoPayload.class)
    FETCHED_CONNECT_SITE_INFO,
    @Action(payloadType = FetchWPComSiteResponsePayload.class)
    FETCHED_WPCOM_SITE_BY_URL,

    // Local actions
    @Action(payloadType = SiteRequestPayload.class)
    UPDATE_SITE,
    @Action(payloadType = SitesRequestPayload.class)
    UPDATE_SITES,
    @Action(payloadType = SiteRequestPayload.class)
    REMOVE_SITE,
    @Action(payloadType = RemoveAllSitesPayload.class)
    REMOVE_ALL_SITES,
    @Action(payloadType = RemoveWpcomAndJetpackSitesPayload.class)
    REMOVE_WPCOM_AND_JETPACK_SITES,
    @Action(payloadType = SitesRequestPayload.class)
    SHOW_SITES,
    @Action(payloadType = SitesRequestPayload.class)
    HIDE_SITES,
    @Action(payloadType = IsWPComResponsePayload.class)
    CHECKED_IS_WPCOM_URL,
    @Action(payloadType = SuggestDomainsResponsePayload.class)
    SUGGESTED_DOMAINS,
}
