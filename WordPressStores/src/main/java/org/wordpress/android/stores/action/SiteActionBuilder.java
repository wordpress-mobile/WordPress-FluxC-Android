package org.wordpress.android.stores.action;

import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.model.SitesModel;
import org.wordpress.android.stores.store.SiteStore.RefreshSitesXMLRPCPayload;

public class SiteActionBuilder {
    public static Action<SiteModel> generateFetchSiteAction(SiteModel payload) {
        return new Action<>(SiteAction.FETCH_SITE, payload);
    }

    public static <T> Action<T> generateFetchSitesAction() {
        return new Action<>(SiteAction.FETCH_SITES, null);
    }

    public static Action<RefreshSitesXMLRPCPayload> generateFetchSitesXmlRpcAction(RefreshSitesXMLRPCPayload payload) {
        return new Action<>(SiteAction.FETCH_SITES_XMLRPC, payload);
    }

    public static Action<SiteModel> generateUpdateSiteAction(SiteModel payload) {
        return new Action<>(SiteAction.UPDATE_SITE, payload);
    }

    public static Action<SitesModel> generateUpdateSitesAction(SitesModel payload) {
        return new Action<>(SiteAction.UPDATE_SITES, payload);
    }

    public static Action<SiteModel> generateRemoveSiteAction(SiteModel payload) {
        return new Action<>(SiteAction.REMOVE_SITE, payload);
    }

    public static <T> Action<T> generateLogoutWpcomAction() {
        return new Action<>(SiteAction.LOGOUT_WPCOM, null);
    }

    public static Action<SitesModel> generateShowSitesAction(SitesModel payload) {
        return new Action<>(SiteAction.SHOW_SITES, payload);
    }

    public static Action<SitesModel> generateHideSitesAction(SitesModel payload) {
        return new Action<>(SiteAction.HIDE_SITES, payload);
    }
}
